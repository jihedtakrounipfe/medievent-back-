package skylinkers.tn.mediconnectbackend.service;

import skylinkers.tn.mediconnectbackend.entities.*;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.ForumComment;
import skylinkers.tn.mediconnectbackend.entities.Notification;
import skylinkers.tn.mediconnectbackend.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ? CONSTRUCTEUR EXPLICITE pour l'injection de d?pendance
    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notifyComment(ForumPost post, ForumComment comment) {
        if (!post.getAuthor().getId().equals(comment.getAuthor().getId())) {
            Notification notification = new Notification();
            notification.setType(Notification.NotificationType.COMMENT);
            notification.setUser(post.getAuthor());
            notification.setActor(comment.getAuthor());
            notification.setPost(post);
            notification.setComment(comment);
            notification.setMessage(buildDisplayName(comment.getAuthor()) + " a comment? votre post");
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyUpvote(ForumComment comment, AppUser voter) {
        if (!comment.getAuthor().getId().equals(voter.getId())) {
            Notification notification = new Notification();
            notification.setType(Notification.NotificationType.UPVOTE);
            notification.setUser(comment.getAuthor());
            notification.setActor(voter);
            notification.setPost(comment.getPost());
            notification.setComment(comment);
            notification.setMessage(buildDisplayName(voter) + " a upvot? votre commentaire");
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyDoctorResponse(ForumPost post, ForumComment comment) {
        if (comment.getAuthor().getUserType() != null && "DOCTOR".equals(comment.getAuthor().getUserType().name())) {
            Notification notification = new Notification();
            notification.setType(Notification.NotificationType.DOCTOR_RESPONSE);
            notification.setUser(post.getAuthor());
            notification.setActor(comment.getAuthor());
            notification.setPost(post);
            notification.setComment(comment);
            notification.setMessage("Dr. " + buildDisplayName(comment.getAuthor()) + " a r?pondu ? votre post");
            notification.setCreatedAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void notifyPinned(ForumPost post, AppUser admin) {
        Notification notification = new Notification();
        notification.setType(Notification.NotificationType.PINNED);
        notification.setUser(post.getAuthor());
        notification.setActor(admin);
        notification.setPost(post);
        notification.setMessage("Votre post a ?t? ?pingl? par un administrateur");
        notification.setCreatedAt(java.time.LocalDateTime.now());
        notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Page<Notification> getUserNotificationsPage(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Transactional
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    private String buildDisplayName(AppUser user) {
        if (user == null) {
            return "Utilisateur";
        }
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String fullName = (first + " " + last).trim();
        return fullName.isEmpty() ? user.getEmail() : fullName;
    }
}
