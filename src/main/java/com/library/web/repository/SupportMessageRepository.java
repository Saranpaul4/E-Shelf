package com.library.web.repository;

import java.util.List;
import java.util.Optional;

import com.library.web.model.SupportMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    /** A reader's whole conversation, oldest first — the chat window reads top to bottom. */
    List<SupportMessage> findByUsernameOrderByCreatedAtAsc(String username);

    /** The most recent line in one conversation, so the admin list can show a preview. */
    Optional<SupportMessage> findFirstByUsernameOrderByCreatedAtDesc(String username);

    long countByUsername(String username);

    /** Every reader who has ever started a conversation — one row per thread on the admin desk. */
    @Query("select distinct m.username from SupportMessage m order by m.username")
    List<String> distinctUsernames();
}
