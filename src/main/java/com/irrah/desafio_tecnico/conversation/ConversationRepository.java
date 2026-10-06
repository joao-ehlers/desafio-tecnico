package com.irrah.desafio_tecnico.conversation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    @EntityGraph(attributePaths = {"client", "recipient"})
    Optional<Conversation> findByIdAndClientId(Long id, Long clientId);

    boolean existsByIdAndClientId(Long id, Long clientId);

    @EntityGraph(attributePaths = {"client", "recipient"})
    Page<Conversation> findByClientId(Long clientId, Pageable pageable);
}