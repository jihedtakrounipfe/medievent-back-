package skylinkers.tn.mediconnectbackend.controller;

import skylinkers.tn.mediconnectbackend.entities.ForumComment;
import skylinkers.tn.mediconnectbackend.entities.ForumPost;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.entities.ForumComment;
import skylinkers.tn.mediconnectbackend.entities.ForumPost;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.repository.ForumCommentRepository;
import skylinkers.tn.mediconnectbackend.repository.ForumPostRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/forum/posts")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class ForumCommentController {

    @Autowired
    private ForumCommentRepository commentRepository;

    @Autowired
    private ForumPostRepository postRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Stockage temporaire des votes utilisateur par commentaire
    // Clé: commentId, Valeur: Set des userId qui ont voté
    private final Map<String, Set<Long>> commentVotes = new ConcurrentHashMap<>();

    // ════════════════════ 1️⃣ GET /api/forum/posts/{id}/comments ════════════════════
    @GetMapping("/{id}/comments")
    public ResponseEntity<List<ForumComment>> getComments(@PathVariable String id) {
        Optional<ForumPost> post = postRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ForumPost forumPost = post.get();
        List<ForumComment> rootComments = commentRepository
                .findByPostAndReplyToIdIsNullOrderByCreatedAtAsc(forumPost);

        List<ForumComment> commentTree = rootComments.stream()
                .map(comment -> buildCommentTree(comment, forumPost.getAuthor().getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(commentTree);
    }

    // ════════════════════ GET avec statut de vote pour l'utilisateur ════════════════════
    @GetMapping("/{id}/comments/with-vote-status")
    public ResponseEntity<Map<String, Object>> getCommentsWithVoteStatus(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<ForumPost> post = postRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ForumPost forumPost = post.get();
        List<ForumComment> rootComments = commentRepository
                .findByPostAndReplyToIdIsNullOrderByCreatedAtAsc(forumPost);

        List<ForumComment> commentTree = rootComments.stream()
                .map(comment -> buildCommentTree(comment, forumPost.getAuthor().getId()))
                .collect(Collectors.toList());

        Map<String, Boolean> voteStatus = new HashMap<>();
        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isPresent()) {
            Long userId = currentUser.get().getId();
            for (ForumComment comment : commentTree) {
                addVoteStatusToCommentTree(comment, userId, voteStatus);
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("comments", commentTree);
        response.put("voteStatus", voteStatus);

        return ResponseEntity.ok(response);
    }

    private void addVoteStatusToCommentTree(ForumComment comment, Long userId, Map<String, Boolean> voteStatus) {
        voteStatus.put(comment.getId(), hasUserVoted(comment.getId(), userId));
        if (comment.getReplies() != null) {
            for (ForumComment reply : comment.getReplies()) {
                addVoteStatusToCommentTree(reply, userId, voteStatus);
            }
        }
    }

    // Helper: Build comment tree with replies
    private ForumComment buildCommentTree(ForumComment comment, Long opId) {
        comment.setAuthorOp(comment.getAuthor().getId().equals(opId));
        comment.setComputedScore(calculateCommentScore(comment));

        List<ForumComment> replies = commentRepository
                .findByReplyToIdOrderByCreatedAtAsc(comment.getId());

        List<ForumComment> nestedReplies = replies.stream()
                .map(reply -> buildCommentTree(reply, opId))
                .collect(Collectors.toList());

        comment.setReplies(nestedReplies);
        return comment;
    }

    // ════════════════════ 2️⃣ POST /api/forum/posts/{id}/comments ════════════════════
    @PostMapping("/{id}/comments")
    public ResponseEntity<ForumComment> addComment(
            @PathVariable String id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<ForumPost> post = postRepository.findById(id);
        if (post.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        ForumComment comment = new ForumComment();
        comment.setContent((String) body.get("content"));
        comment.setPost(post.get());
        comment.setCreatedAt(LocalDateTime.now());  // ✅ Ajouter la date de création
        comment.setUpdatedAt(LocalDateTime.now());   // ✅ Ajouter la date de mise à jour

        if (body.containsKey("replyToId") && body.get("replyToId") != null) {
            String replyToId = (String) body.get("replyToId");
            Optional<ForumComment> parentComment = commentRepository.findById(replyToId);
            if (parentComment.isPresent()) {
                comment.setReplyToId(replyToId);
            }
        }

        comment.setAuthor(currentUser.get());
        comment.setUpvotes(0);
        comment.setDownvotes(0);
        comment.setAnswer(false);
        comment.setHelpfulCount(0);
        comment.setSolutionCount(0);
        comment.setIrrelevantCount(0);
        comment.setAuthorReputation(0);
        comment.setEditHistory("[]");

        ForumPost forumPost = post.get();
        forumPost.setCommentCount(forumPost.getCommentCount() + 1);
        postRepository.save(forumPost);

        ForumComment saved = commentRepository.save(comment);

        // ✅ IMPORTANT: Forcer le flush pour générer l'ID
        commentRepository.flush();

        System.out.println("✅ Commentaire créé avec ID: " + saved.getId());
        System.out.println("✅ Commentaire complet: " + saved);

        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    // ════════════════════ 3️⃣ PUT /api/forum/posts/{postId}/comments/{commentId} ════════════════════
    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<ForumComment> updateComment(
            @PathVariable String postId,
            @PathVariable String commentId,
            @RequestBody Map<String, String> body) {

        Optional<ForumComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ForumComment comment = commentOpt.get();
        String oldContent = comment.getContent();

        try {
            List<Map<String, Object>> history = new ArrayList<>();
            if (!comment.getEditHistory().isEmpty() && !comment.getEditHistory().equals("[]")) {
                history = objectMapper.readValue(
                        comment.getEditHistory(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
            }

            Map<String, Object> editEntry = new LinkedHashMap<>();
            editEntry.put("version", history.size() + 1);
            editEntry.put("content", oldContent);
            editEntry.put("editedAt", LocalDateTime.now());

            history.add(editEntry);
            comment.setEditHistory(objectMapper.writeValueAsString(history));
        } catch (Exception e) {
            System.err.println("Error saving edit history: " + e.getMessage());
        }

        comment.setContent(body.get("content"));
        comment.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(commentRepository.save(comment));
    }

    // ════════════════════ 4️⃣ PATCH /api/forum/posts/{postId}/comments/{commentId}/utility ════════════════════
    @PatchMapping("/{postId}/comments/{commentId}/utility")
    public ResponseEntity<Map<String, Object>> markCommentUtility(
            @PathVariable String postId,
            @PathVariable String commentId,
            @RequestBody Map<String, String> body) {

        Optional<ForumComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ForumComment comment = commentOpt.get();
        String utilityType = body.get("type");

        if ("helpful".equals(utilityType)) {
            comment.setHelpfulCount(comment.getHelpfulCount() + 1);
            comment.setAuthorReputation(comment.getAuthorReputation() + 3);
        } else if ("solution".equals(utilityType)) {
            comment.setSolutionCount(comment.getSolutionCount() + 1);
            comment.setAnswer(true);
            comment.setAuthorReputation(comment.getAuthorReputation() + 10);
        } else if ("irrelevant".equals(utilityType)) {
            comment.setIrrelevantCount(comment.getIrrelevantCount() + 1);
            comment.setAuthorReputation(Math.max(0, comment.getAuthorReputation() - 2));
        }

        ForumComment saved = commentRepository.save(comment);

        return ResponseEntity.ok(Map.of(
                "comment", saved,
                "message", "Utilité marquée avec succès",
                "reputationAdded", true
        ));
    }

    // ════════════════════ 5️⃣ GET /api/forum/posts/{postId}/comments/{commentId}/history ════════════════════
    @GetMapping("/{postId}/comments/{commentId}/history")
    public ResponseEntity<List<Map<String, Object>>> getCommentEditHistory(
            @PathVariable String postId,
            @PathVariable String commentId) {

        Optional<ForumComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ForumComment comment = commentOpt.get();
        List<Map<String, Object>> history = new ArrayList<>();

        try {
            if (!comment.getEditHistory().isEmpty() && !comment.getEditHistory().equals("[]")) {
                history = objectMapper.readValue(
                        comment.getEditHistory(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class)
                );
            }

            Map<String, Object> currentVersion = new LinkedHashMap<>();
            currentVersion.put("version", history.size() + 1);
            currentVersion.put("content", comment.getContent());
            currentVersion.put("editedAt", comment.getUpdatedAt());
            currentVersion.put("isCurrent", true);

            history.add(currentVersion);

        } catch (Exception e) {
            System.err.println("Error parsing edit history: " + e.getMessage());
        }

        return ResponseEntity.ok(history);
    }

    // ════════════════════ 6️⃣ TOGGLE UPVOTE (AJOUTER/RETIRER LE VOTE) ════════════════════
    @PatchMapping("/{postId}/comments/{commentId}/upvote")
    @PostMapping("/{postId}/comments/{commentId}/upvote")
    public ResponseEntity<Map<String, Object>> toggleUpvote(
            @PathVariable String postId,
            @PathVariable String commentId,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<ForumComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = currentUser.get().getId();

        ForumComment comment = commentOpt.get();
        boolean alreadyUpvoted = hasUserVoted(commentId, userId);

        if (alreadyUpvoted) {
            comment.setUpvotes(Math.max(0, comment.getUpvotes() - 1));
            removeUserVote(commentId, userId);
            commentRepository.save(comment);

            return ResponseEntity.ok(Map.of(
                    "upvotes", comment.getUpvotes(),
                    "voted", false,
                    "message", "Vote retiré"
            ));
        } else {
            comment.setUpvotes(comment.getUpvotes() + 1);
            addUserVote(commentId, userId);
            commentRepository.save(comment);

            return ResponseEntity.ok(Map.of(
                    "upvotes", comment.getUpvotes(),
                    "voted", true,
                    "message", "Vote ajouté"
            ));
        }
    }
    // ════════════════════ 7️⃣ PATCH /api/forum/posts/{postId}/comments/{commentId}/mark-answer ════════════════════
    @PatchMapping("/{postId}/comments/{commentId}/mark-answer")
    public ResponseEntity<ForumComment> markAsAnswer(
            @PathVariable String postId,
            @PathVariable String commentId) {

        Optional<ForumComment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ForumComment c = comment.get();
        c.setAnswer(true);
        c.setSolutionCount(c.getSolutionCount() + 1);
        c.setAuthorReputation(c.getAuthorReputation() + 10);

        return ResponseEntity.ok(commentRepository.save(c));
    }

    // ════════════════════ 8️⃣ DELETE /api/forum/posts/{postId}/comments/{commentId} ════════════════════
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable String postId,
            @PathVariable String commentId) {

        Optional<ForumComment> comment = commentRepository.findById(commentId);
        if (comment.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Supprimer aussi les votes associés
        commentVotes.remove(commentId);

        // Decrement post comment count
        Optional<ForumPost> post = postRepository.findById(postId);
        if (post.isPresent()) {
            ForumPost forumPost = post.get();
            forumPost.setCommentCount(Math.max(0, forumPost.getCommentCount() - 1));
            postRepository.save(forumPost);
        }

        commentRepository.deleteById(commentId);
        return ResponseEntity.noContent().build();
    }

    // ════════════════════ 9️⃣ GET vote status pour un commentaire ════════════════════
    @GetMapping("/{postId}/comments/{commentId}/vote-status")
    public ResponseEntity<Map<String, Object>> getVoteStatus(
            @PathVariable String postId,
            @PathVariable String commentId,
            @AuthenticationPrincipal Jwt jwt) {

        Optional<ForumComment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = currentUser.get().getId();

        ForumComment comment = commentOpt.get();
        boolean voted = hasUserVoted(commentId, userId);

        return ResponseEntity.ok(Map.of(
                "upvotes", comment.getUpvotes(),
                "voted", voted
        ));
    }

    // ════════════════════ MÉTHODES PRIVÉES POUR LA GESTION DES VOTES ════════════════════

    private boolean hasUserVoted(String commentId, Long userId) {
        return commentVotes.containsKey(commentId) &&
                commentVotes.get(commentId).contains(userId);
    }

    private void addUserVote(String commentId, Long userId) {
        commentVotes.computeIfAbsent(commentId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    private void removeUserVote(String commentId, Long userId) {
        if (commentVotes.containsKey(commentId)) {
            commentVotes.get(commentId).remove(userId);
            if (commentVotes.get(commentId).isEmpty()) {
                commentVotes.remove(commentId);
            }
        }
    }

    private Optional<AppUser> resolveCurrentUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByKeycloakId(jwt.getSubject());
    }

    // ════════════════════ HELPER: Calculate Comment Score ════════════════════
    private double calculateCommentScore(ForumComment comment) {
        double score = comment.getUpvotes() * 1.5;

        if (comment.getAuthor() != null &&
                comment.getAuthor().getUserType() != null &&
                "DOCTOR".equals(comment.getAuthor().getUserType().name())) {
            score += 3;
        }

        if (comment.isAnswer()) {
            score += 5;
        }

        score += comment.getHelpfulCount() * 2;
        score += comment.getSolutionCount() * 5;
        score -= comment.getIrrelevantCount() * 3;

        if (comment.getCreatedAt() != null) {
            long daysOld = java.time.temporal.ChronoUnit.DAYS
                    .between(comment.getCreatedAt(), LocalDateTime.now());
            double recencyBonus = Math.max(0, 2 - (daysOld / 7.0));
            score += recencyBonus;
        }

        score += comment.getAuthorReputation() * 0.1;

        return Math.max(0, Math.round(score * 10) / 10.0);
    }
}