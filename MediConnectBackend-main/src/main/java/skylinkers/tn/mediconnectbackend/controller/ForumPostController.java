package skylinkers.tn.mediconnectbackend.controller;

import jakarta.transaction.Transactional;
import skylinkers.tn.mediconnectbackend.entities.ForumAttachment;
import skylinkers.tn.mediconnectbackend.entities.ForumPost;
import skylinkers.tn.mediconnectbackend.entities.PostReaction;
import skylinkers.tn.mediconnectbackend.entities.PostReactionId;
import skylinkers.tn.mediconnectbackend.entities.UserSavedPost;
import skylinkers.tn.mediconnectbackend.entities.UserSavedPostId;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.repository.ForumAttachmentRepository;
import skylinkers.tn.mediconnectbackend.repository.ForumCommentRepository;
import skylinkers.tn.mediconnectbackend.repository.ForumPostRepository;
import skylinkers.tn.mediconnectbackend.repository.PostReactionRepository;
import skylinkers.tn.mediconnectbackend.repository.UserSavedPostRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/forum")
@CrossOrigin(origins = {"http://localhost:4200"})
public class ForumPostController {
    @Autowired
    private ForumCommentRepository commentRepository;
    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private ForumAttachmentRepository attachmentRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private UserSavedPostRepository userSavedPostRepository;

    @Autowired
    private PostReactionRepository postReactionRepository;

    private final String UPLOAD_DIR = "uploads/";

    @GetMapping("/test")
    public String test() {
        return "API is working!";
    }

    @GetMapping("/posts")
    @Transactional
    public ResponseEntity<Map<String, Object>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ForumPost> postsPage = postRepository.findAll(pageable);

        List<Map<String, Object>> postsWithAttachments = new ArrayList<>();

        for (ForumPost post : postsPage.getContent()) {
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", post.getId());
            postMap.put("title", post.getTitle());
            postMap.put("content", post.getContent());
            postMap.put("category", post.getCategory());
            postMap.put("author", buildAuthorMap(post.getAuthor()));
            postMap.put("createdAt", post.getCreatedAt());
            postMap.put("viewCount", post.getViewCount());
            postMap.put("commentCount", post.getCommentCount());
            postMap.put("isVerifiedByDoctor", post.isVerifiedByDoctor());
            postMap.put("tags", post.getTags());
            postMap.put("pinned", post.isPinned());

            List<ForumAttachment> attachments = attachmentRepository.findByPostId(post.getId());
            List<Map<String, Object>> attachmentList = new ArrayList<>();
            for (ForumAttachment att : attachments) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("url", att.getUrl());
                attMap.put("name", att.getFileName());
                attMap.put("type", att.getFileType());
                attMap.put("size", att.getFileSize());
                attMap.put("category", att.getCategory());
                attachmentList.add(attMap);
            }
            postMap.put("attachments", attachmentList);

            postsWithAttachments.add(postMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", postsWithAttachments);
        response.put("totalElements", postsPage.getTotalElements());
        response.put("totalPages", postsPage.getTotalPages());
        response.put("size", postsPage.getSize());
        response.put("number", postsPage.getNumber());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<Map<String, Object>> getPostById(@PathVariable String id) {
        Optional<ForumPost> postOpt = postRepository.findById(id);
        if (!postOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        ForumPost post = postOpt.get();
        Map<String, Object> postMap = new HashMap<>();
        postMap.put("id", post.getId());
        postMap.put("title", post.getTitle());
        postMap.put("content", post.getContent());
        postMap.put("category", post.getCategory());
        postMap.put("author", buildAuthorMap(post.getAuthor()));

        postMap.put("createdAt", post.getCreatedAt());
        postMap.put("viewCount", post.getViewCount());
        postMap.put("commentCount", post.getCommentCount());
        postMap.put("isVerifiedByDoctor", post.isVerifiedByDoctor());
        postMap.put("tags", post.getTags());
        postMap.put("pinned", post.isPinned());

        List<ForumAttachment> attachments = attachmentRepository.findByPostId(post.getId());
        List<Map<String, Object>> attachmentList = new ArrayList<>();
        for (ForumAttachment att : attachments) {
            Map<String, Object> attMap = new HashMap<>();
            attMap.put("url", att.getUrl());
            attMap.put("name", att.getFileName());
            attMap.put("type", att.getFileType());
            attMap.put("size", att.getFileSize());
            attMap.put("category", att.getCategory());
            attachmentList.add(attMap);
        }
        postMap.put("attachments", attachmentList);

        return ResponseEntity.ok(postMap);
    }

    @PostMapping("/posts")
    public ResponseEntity<Map<String, Object>> createPost(
            @RequestBody Map<String, Object> postData,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            Optional<AppUser> currentUser = resolveCurrentUser(jwt);
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            ForumPost post = new ForumPost();
            post.setTitle((String) postData.get("title"));
            post.setContent((String) postData.get("content"));
            post.setCategory(ForumPost.PostCategory.valueOf((String) postData.get("category")));
            post.setAuthor(currentUser.get());
            post.setVerifiedByDoctor(false);
            post.setViewCount(0);
            post.setCommentCount(0);
            post.setSaveCount(0);

            ForumPost savedPost = postRepository.save(post);

            Map<String, Object> response = new HashMap<>();
            response.put("id", savedPost.getId());
            response.put("title", savedPost.getTitle());
            response.put("content", savedPost.getContent());
            response.put("category", savedPost.getCategory());
            response.put("author", buildAuthorMap(savedPost.getAuthor()));
            response.put("createdAt", savedPost.getCreatedAt());
            response.put("viewCount", savedPost.getViewCount());
            response.put("commentCount", savedPost.getCommentCount());
            response.put("isVerifiedByDoctor", savedPost.isVerifiedByDoctor());
            response.put("tags", savedPost.getTags());
            response.put("pinned", savedPost.isPinned());
            response.put("attachments", new ArrayList<>());

            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping(value = "/posts/with-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> createPostWithAttachments(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestParam(value = "authorId", required = false) Long authorId,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @AuthenticationPrincipal Jwt jwt) {

        System.out.println("=== DÉBUT TRAITEMENT ===");
        System.out.println("1. title: " + title);
        System.out.println("2. content: " + content);
        System.out.println("3. category: " + category);
        System.out.println("4. authorId (ignored): " + authorId);
        System.out.println("5. tags: " + tags);
        System.out.println("6. files: " + (files != null ? files.length : 0));

        try {
            Optional<AppUser> currentUser = resolveCurrentUser(jwt);
            if (currentUser.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Étape 1: Vérifier la catégorie
            ForumPost.PostCategory postCategory;
            try {
                postCategory = ForumPost.PostCategory.valueOf(category);
                System.out.println("✅ Catégorie OK: " + postCategory);
            } catch (IllegalArgumentException e) {
                System.out.println("❌ ERREUR CATÉGORIE: " + category);
                return ResponseEntity.badRequest().build();
            }

            // Étape 2: Créer le post
            ForumPost post = new ForumPost();
            post.setTitle(title);
            post.setContent(content);
            post.setCategory(postCategory);
            System.out.println("✅ Post créé");

            // Étape 3: Gérer les tags
            if (tags != null && !tags.isEmpty()) {
                try {
                    String cleanTags = tags.replace("[", "").replace("]", "").replace("\"", "");
                    List<String> tagList = Arrays.asList(cleanTags.split(","));
                    tagList.replaceAll(String::trim);
                    post.setTags(tagList);
                    System.out.println("✅ Tags ajoutés: " + tagList);
                } catch (Exception e) {
                    System.out.println("⚠️ Erreur tags: " + e.getMessage());
                }
            }

            // Étape 4: Récupérer l'utilisateur authentifié
            post.setAuthor(currentUser.get());
            System.out.println("✅ Utilisateur authentifié trouvé: " + currentUser.get().getEmail());

            // ✅ AJOUTER saveCount ICI - CORRECTION CLÉ !
            post.setSaveCount(0);
            post.setViewCount(0);
            post.setCommentCount(0);

            // Étape 5: Sauvegarder le post
            ForumPost savedPost = postRepository.save(post);
            System.out.println("✅ Post sauvegardé avec ID: " + savedPost.getId());

            // Étape 6: Gérer les fichiers
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("✅ Dossier uploads créé");
            }

            List<Map<String, Object>> uploadedFiles = new ArrayList<>();

            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String originalFileName = file.getOriginalFilename();
                        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
                        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                        Path filePath = uploadPath.resolve(uniqueFileName);

                        try {
                            Files.copy(file.getInputStream(), filePath);
                            System.out.println("✅ Fichier sauvegardé: " + uniqueFileName);
                        } catch (IOException e) {
                            System.out.println("❌ Erreur sauvegarde fichier: " + e.getMessage());
                            e.printStackTrace();
                            return ResponseEntity.badRequest().build();
                        }

                        String fileUrl = "/uploads/" + uniqueFileName;
                        String fileType = file.getContentType();
                        String fileCategory;

                        if (fileExtension.equals(".pdf")) {
                            fileCategory = "pdf";
                        } else if (fileExtension.equals(".gif")) {
                            fileCategory = "gif";
                        } else if (fileType != null && fileType.startsWith("image/")) {
                            fileCategory = "image";
                        } else if (fileType != null && fileType.startsWith("video/")) {
                            fileCategory = "video";
                        } else {
                            fileCategory = "other";
                        }

                        ForumAttachment attachment = new ForumAttachment();
                        attachment.setPost(savedPost);
                        attachment.setUrl(fileUrl);
                        attachment.setFileName(originalFileName);
                        attachment.setFileType(fileType);
                        attachment.setFileSize(file.getSize());
                        attachment.setCategory(fileCategory);

                        try {
                            attachmentRepository.save(attachment);
                            System.out.println("✅ Attachment sauvegardé: " + originalFileName);
                        } catch (Exception e) {
                            System.out.println("❌ Erreur sauvegarde attachment: " + e.getMessage());
                            e.printStackTrace();
                        }

                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("url", fileUrl);
                        fileInfo.put("name", originalFileName);
                        fileInfo.put("type", fileType);
                        fileInfo.put("size", file.getSize());
                        fileInfo.put("category", fileCategory);
                        uploadedFiles.add(fileInfo);
                    }
                }
            }

            // Étape 7: Construire la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("id", savedPost.getId());
            response.put("title", savedPost.getTitle());
            response.put("content", savedPost.getContent());
            response.put("category", savedPost.getCategory());
            response.put("author", buildAuthorMap(post.getAuthor()));
            response.put("viewCount", savedPost.getViewCount());
            response.put("commentCount", savedPost.getCommentCount());
            response.put("isVerifiedByDoctor", savedPost.isVerifiedByDoctor());
            response.put("tags", savedPost.getTags());
            response.put("pinned", savedPost.isPinned());
            response.put("attachments", uploadedFiles);

            System.out.println("=== SUCCÈS TOTAL ===");
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (Exception e) {
            System.err.println("=== ERREUR GLOBALE ===");
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/posts/pinned")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> getPinnedPosts() {
        List<ForumPost> pinnedPosts = postRepository.findByPinnedTrue();
        List<Map<String, Object>> postsWithAttachments = new ArrayList<>();

        for (ForumPost post : pinnedPosts) {
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", post.getId());
            postMap.put("title", post.getTitle());
            postMap.put("content", post.getContent());
            postMap.put("category", post.getCategory());
            postMap.put("author", buildAuthorMap(post.getAuthor()));            postMap.put("createdAt", post.getCreatedAt());
            postMap.put("viewCount", post.getViewCount());
            postMap.put("commentCount", post.getCommentCount());
            postMap.put("isVerifiedByDoctor", post.isVerifiedByDoctor());
            postMap.put("tags", post.getTags());
            postMap.put("pinned", post.isPinned());

            List<ForumAttachment> attachments = attachmentRepository.findByPostId(post.getId());
            List<Map<String, Object>> attachmentList = new ArrayList<>();
            for (ForumAttachment att : attachments) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("url", att.getUrl());
                attMap.put("name", att.getFileName());
                attMap.put("type", att.getFileType());
                attMap.put("size", att.getFileSize());
                attMap.put("category", att.getCategory());
                attachmentList.add(attMap);
            }
            postMap.put("attachments", attachmentList);

            postsWithAttachments.add(postMap);
        }

        return ResponseEntity.ok(postsWithAttachments);
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<ForumPost> updatePost(@PathVariable String id, @RequestBody Map<String, Object> postData) {
        Optional<ForumPost> existingPost = postRepository.findById(id);
        if (existingPost.isPresent()) {
            ForumPost post = existingPost.get();
            if (postData.containsKey("title")) {
                post.setTitle((String) postData.get("title"));
            }
            if (postData.containsKey("content")) {
                post.setContent((String) postData.get("content"));
            }
            if (postData.containsKey("category")) {
                String categoryStr = (String) postData.get("category");
                post.setCategory(ForumPost.PostCategory.valueOf(categoryStr));
            }
            if (postData.containsKey("tags")) {
                @SuppressWarnings("unchecked")
                List<String> tagsList = (List<String>) postData.get("tags");
                post.setTags(tagsList);
            }

            ForumPost updatedPost = postRepository.save(post);
            return ResponseEntity.ok(updatedPost);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping(value = "/posts/{id}/with-files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> updatePostWithFiles(
            @PathVariable String id,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam("category") String category,
            @RequestParam("authorId") Long authorId,
            @RequestParam(value = "tags", required = false) String tags,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            @RequestParam(value = "filesToDelete", required = false) String filesToDelete) {

        try {
            System.out.println("=== MISE À JOUR POST ===");
            System.out.println("ID: " + id);
            System.out.println("Title: " + title);
            System.out.println("Category: " + category);
            System.out.println("Tags: " + tags);

            Optional<ForumPost> existingPost = postRepository.findById(id);
            if (!existingPost.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            ForumPost post = existingPost.get();

            // Mettre à jour les champs texte
            post.setTitle(title);
            post.setContent(content);
            post.setCategory(ForumPost.PostCategory.valueOf(category));

            // GESTION CORRECTE DES TAGS - Créer une nouvelle ArrayList mutable
            List<String> newTags = new ArrayList<>();
            if (tags != null && !tags.isEmpty() && !tags.equals("[]")) {
                String cleanTags = tags.replace("[", "").replace("]", "").replace("\"", "");
                if (!cleanTags.isEmpty()) {
                    String[] tagArray = cleanTags.split(",");
                    for (String tag : tagArray) {
                        String trimmedTag = tag.trim();
                        if (!trimmedTag.isEmpty()) {
                            newTags.add(trimmedTag);
                        }
                    }
                }
            }
            post.setTags(newTags);

            postRepository.save(post);

            // Supprimer les fichiers demandés
            if (filesToDelete != null && !filesToDelete.isEmpty() && !filesToDelete.equals("[]")) {
                String cleanDeleteIds = filesToDelete.replace("[", "").replace("]", "").replace("\"", "");
                if (!cleanDeleteIds.isEmpty()) {
                    String[] deleteIds = cleanDeleteIds.split(",");
                    for (String deleteId : deleteIds) {
                        String idToDelete = deleteId.trim();
                        if (!idToDelete.isEmpty()) {
                            Optional<ForumAttachment> attOpt = attachmentRepository.findById(idToDelete);
                            if (attOpt.isPresent()) {
                                ForumAttachment att = attOpt.get();
                                String fileName = att.getUrl().replace("/uploads/", "");
                                Path filePath = Paths.get(UPLOAD_DIR + fileName);
                                try {
                                    Files.deleteIfExists(filePath);
                                    System.out.println("Fichier supprimé: " + fileName);
                                } catch (IOException e) {
                                    System.err.println("Erreur suppression fichier: " + e.getMessage());
                                }
                                attachmentRepository.delete(att);
                            }
                        }
                    }
                }
            }

            // Ajouter les nouveaux fichiers
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            if (files != null && files.length > 0) {
                for (MultipartFile file : files) {
                    if (file != null && !file.isEmpty()) {
                        String originalFileName = file.getOriginalFilename();
                        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
                        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                        Path filePath = uploadPath.resolve(uniqueFileName);
                        Files.copy(file.getInputStream(), filePath);

                        String fileUrl = "/uploads/" + uniqueFileName;
                        String fileType = file.getContentType();
                        String fileCategory;

                        if (fileExtension.equals(".pdf")) {
                            fileCategory = "pdf";
                        } else if (fileExtension.equals(".gif")) {
                            fileCategory = "gif";
                        } else if (fileType != null && fileType.startsWith("image/")) {
                            fileCategory = "image";
                        } else if (fileType != null && fileType.startsWith("video/")) {
                            fileCategory = "video";
                        } else {
                            fileCategory = "other";
                        }

                        ForumAttachment attachment = new ForumAttachment();
                        attachment.setPost(post);
                        attachment.setUrl(fileUrl);
                        attachment.setFileName(originalFileName);
                        attachment.setFileType(fileType);
                        attachment.setFileSize(file.getSize());
                        attachment.setCategory(fileCategory);
                        attachmentRepository.save(attachment);

                        System.out.println("Nouveau fichier ajouté: " + originalFileName + " -> " + fileCategory);
                    }
                }
            }

            // Récupérer tous les attachments actuels pour la réponse
            List<ForumAttachment> currentAttachments = attachmentRepository.findByPostId(id);
            List<Map<String, Object>> attachmentList = new ArrayList<>();
            for (ForumAttachment att : currentAttachments) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("id", att.getId());
                attMap.put("url", att.getUrl());
                attMap.put("name", att.getFileName());
                attMap.put("type", att.getFileType());
                attMap.put("size", att.getFileSize());
                attMap.put("category", att.getCategory());
                attachmentList.add(attMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", post.getId());
            response.put("title", post.getTitle());
            response.put("content", post.getContent());
            response.put("category", post.getCategory());
            response.put("author", buildAuthorMap(post.getAuthor()));            response.put("createdAt", post.getCreatedAt());
            response.put("viewCount", post.getViewCount());
            response.put("commentCount", post.getCommentCount());
            response.put("isVerifiedByDoctor", post.isVerifiedByDoctor());
            response.put("tags", post.getTags());
            response.put("pinned", post.isPinned());
            response.put("attachments", attachmentList);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @Transactional
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable String id) {
        System.out.println("🔍 Tentative de suppression du post: " + id);

        Optional<ForumPost> existingPost = postRepository.findById(id);
        if (existingPost.isEmpty()) {
            System.out.println("❌ Post non trouvé: " + id);
            return ResponseEntity.notFound().build();
        }

        try {
            ForumPost post = existingPost.get();
            System.out.println("✅ Post trouvé: " + post.getTitle());

            postReactionRepository.deleteByPostId(id);
            userSavedPostRepository.deleteByPostId(id);
            commentRepository.deleteByPostId(id);
            attachmentRepository.deleteByPostId(id);
            postRepository.delete(post);

            System.out.println("✅ Post supprimé avec succès: " + id);
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            System.err.println("❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/posts/search")
    @Transactional
    public ResponseEntity<Map<String, Object>> searchPosts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ForumPost> postsPage = postRepository.findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(q, q, pageable);

        List<Map<String, Object>> postsWithAttachments = new ArrayList<>();
        for (ForumPost post : postsPage.getContent()) {
            Map<String, Object> postMap = new HashMap<>();
            postMap.put("id", post.getId());
            postMap.put("title", post.getTitle());
            postMap.put("content", post.getContent());
            postMap.put("category", post.getCategory());
            postMap.put("author", buildAuthorMap(post.getAuthor()));            postMap.put("createdAt", post.getCreatedAt());
            postMap.put("viewCount", post.getViewCount());
            postMap.put("commentCount", post.getCommentCount());
            postMap.put("isVerifiedByDoctor", post.isVerifiedByDoctor());
            postMap.put("tags", post.getTags());
            postMap.put("pinned", post.isPinned());

            List<ForumAttachment> attachments = attachmentRepository.findByPostId(post.getId());
            List<Map<String, Object>> attachmentList = new ArrayList<>();
            for (ForumAttachment att : attachments) {
                Map<String, Object> attMap = new HashMap<>();
                attMap.put("url", att.getUrl());
                attMap.put("name", att.getFileName());
                attMap.put("type", att.getFileType());
                attMap.put("size", att.getFileSize());
                attMap.put("category", att.getCategory());
                attachmentList.add(attMap);
            }
            postMap.put("attachments", attachmentList);

            postsWithAttachments.add(postMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", postsWithAttachments);
        response.put("totalElements", postsPage.getTotalElements());
        response.put("totalPages", postsPage.getTotalPages());
        response.put("size", postsPage.getSize());
        response.put("number", postsPage.getNumber());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/posts/{id}/save")
    public ResponseEntity<Map<String, Object>> savePost(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<ForumPost> postOpt = postRepository.findById(id);
        if (postOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = currentUser.get().getId();

        ForumPost post = postOpt.get();

        if (!userSavedPostRepository.existsByPostIdAndUserId(id, userId)) {
            UserSavedPost savedPost = new UserSavedPost();
            savedPost.setId(new UserSavedPostId(userId, id));
            savedPost.setUser(currentUser.get());
            savedPost.setPost(post);
            userSavedPostRepository.save(savedPost);
        }

        return ResponseEntity.ok(Map.of("saved", true));
    }

    @DeleteMapping("/posts/{id}/save")
    public ResponseEntity<Map<String, Object>> unsavePost(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = currentUser.get().getId();

        UserSavedPostId savedPostId = new UserSavedPostId(userId, id);
        if (userSavedPostRepository.existsById(savedPostId)) {
            userSavedPostRepository.deleteById(savedPostId);
        }

        return ResponseEntity.ok(Map.of("saved", false));
    }

    @GetMapping("/posts/{id}/save-status")
    public ResponseEntity<Map<String, Object>> getSaveStatus(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = currentUser.get().getId();

        boolean saved = userSavedPostRepository.existsByPostIdAndUserId(id, userId);
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    @GetMapping("/posts/{id}/reactions")
    public ResponseEntity<Map<String, Object>> getReactions(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        if (postRepository.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Long> counts = getReactionCounts(id);
        String userReaction = null;

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isPresent()) {
            Long userId = currentUser.get().getId();
            Optional<PostReaction> reaction = postReactionRepository.findByPostIdAndUserId(id, userId);
            if (reaction.isPresent()) {
                userReaction = reaction.get().getEmoji();
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("counts", counts);
        response.put("userReaction", userReaction);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/posts/{id}/reactions")
    public ResponseEntity<Map<String, Object>> setReaction(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<ForumPost> postOpt = postRepository.findById(id);
        if (postOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = currentUser.get().getId();

        Object emojiValue = body.get("emoji");
        if (!(emojiValue instanceof String) || ((String) emojiValue).trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        String emoji = ((String) emojiValue).trim();

        ForumPost post = postOpt.get();

        Optional<PostReaction> existing = postReactionRepository.findByPostIdAndUserId(id, userId);
        PostReaction reaction = existing.orElseGet(PostReaction::new);
        reaction.setId(new PostReactionId(userId, id));
        reaction.setUser(currentUser.get());
        reaction.setPost(post);
        reaction.setEmoji(emoji);
        postReactionRepository.save(reaction);

        Map<String, Long> counts = getReactionCounts(id);
        return ResponseEntity.ok(Map.of(
                "counts", counts,
                "userReaction", emoji
        ));
    }

    @DeleteMapping("/posts/{id}/reactions")
    public ResponseEntity<Map<String, Object>> clearReaction(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = currentUser.get().getId();

        PostReactionId reactionId = new PostReactionId(userId, id);
        if (postReactionRepository.existsById(reactionId)) {
            postReactionRepository.deleteById(reactionId);
        }

        Map<String, Long> counts = getReactionCounts(id);
        return ResponseEntity.ok(Map.of(
                "counts", counts,
                "userReaction", null
        ));
    }

    private Map<String, Long> getReactionCounts(String postId) {
        List<Object[]> rows = postReactionRepository.countByEmojiForPost(postId);
        Map<String, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            String emoji = (String) row[0];
            Long count = 0L;
            if (row[1] instanceof Number) {
                count = ((Number) row[1]).longValue();
            }
            counts.put(emoji, count);
        }
        return counts;
    }

    private Optional<AppUser> resolveCurrentUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByKeycloakId(jwt.getSubject());
    }

    @GetMapping("/posts/category/{category}")
    @Transactional
    public ResponseEntity<Map<String, Object>> getPostsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            ForumPost.PostCategory cat = ForumPost.PostCategory.valueOf(category.toUpperCase());
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            Page<ForumPost> postsPage = postRepository.findByCategory(cat, pageable);

            List<Map<String, Object>> postsWithAttachments = new ArrayList<>();
            for (ForumPost post : postsPage.getContent()) {
                Map<String, Object> postMap = new HashMap<>();
                postMap.put("id", post.getId());
                postMap.put("title", post.getTitle());
                postMap.put("content", post.getContent());
                postMap.put("category", post.getCategory());
                postMap.put("author", buildAuthorMap(post.getAuthor()));                postMap.put("createdAt", post.getCreatedAt());
                postMap.put("viewCount", post.getViewCount());
                postMap.put("commentCount", post.getCommentCount());
                postMap.put("isVerifiedByDoctor", post.isVerifiedByDoctor());
                postMap.put("tags", post.getTags());
                postMap.put("pinned", post.isPinned());

                List<ForumAttachment> attachments = attachmentRepository.findByPostId(post.getId());
                List<Map<String, Object>> attachmentList = new ArrayList<>();
                for (ForumAttachment att : attachments) {
                    Map<String, Object> attMap = new HashMap<>();
                    attMap.put("url", att.getUrl());
                    attMap.put("name", att.getFileName());
                    attMap.put("type", att.getFileType());
                    attMap.put("size", att.getFileSize());
                    attMap.put("category", att.getCategory());
                    attachmentList.add(attMap);
                }
                postMap.put("attachments", attachmentList);

                postsWithAttachments.add(postMap);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("content", postsWithAttachments);
            response.put("totalElements", postsPage.getTotalElements());
            response.put("totalPages", postsPage.getTotalPages());
            response.put("size", postsPage.getSize());
            response.put("number", postsPage.getNumber());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/posts/{id}/pin")
    public ResponseEntity<Void> pinPost(@PathVariable String id) {
        Optional<ForumPost> postOpt = postRepository.findById(id);
        if (postOpt.isPresent()) {
            ForumPost post = postOpt.get();
            post.setPinned(true);
            postRepository.save(post);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/posts/{id}/unpin")
    public ResponseEntity<Void> unpinPost(@PathVariable String id) {
        Optional<ForumPost> postOpt = postRepository.findById(id);
        if (postOpt.isPresent()) {
            ForumPost post = postOpt.get();
            post.setPinned(false);
            postRepository.save(post);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
    private Map<String, Object> buildAuthorMap(AppUser author) {
        if (author == null) return null;
        Map<String, Object> map = new HashMap<>();
        map.put("id", author.getId());
        map.put("firstName", author.getFirstName());
        map.put("lastName", author.getLastName());
        map.put("email", author.getEmail());
        map.put("userType", author.getUserType());
        map.put("profilePicture", author.getProfilePicture());
        return map;
    }
}