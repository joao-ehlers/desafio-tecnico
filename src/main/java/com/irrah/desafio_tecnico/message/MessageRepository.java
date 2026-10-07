package com.irrah.desafio_tecnico.message;

import com.irrah.desafio_tecnico.message.projection.LatestMessageView;
import com.irrah.desafio_tecnico.message.projection.UnreadCountView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender", "conversation", "conversation.recipient"})
    @Query("""
            select m from Message m
            where m.id = :messageId
              and m.sender.id = :clientId
              and m.conversation.client.id = :clientId
            """)
    Optional<Message> findOwnedMessage(
            @Param("messageId") Long messageId, @Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"sender", "conversation", "conversation.recipient"})
    @Query(value = """
            select m from Message m
            where m.sender.id = :clientId
              and m.conversation.client.id = :clientId
              and (:conversationId is null or m.conversation.id = :conversationId)
              and (:status is null or m.status = :status)
              and (:priority is null or m.priority = :priority)
              and (:channel is null or m.channel = :channel)
            """, countQuery = """
            select count(m) from Message m
            where m.sender.id = :clientId
              and m.conversation.client.id = :clientId
              and (:conversationId is null or m.conversation.id = :conversationId)
              and (:status is null or m.status = :status)
              and (:priority is null or m.priority = :priority)
              and (:channel is null or m.channel = :channel)
            """)
    Page<Message> searchOwnedMessages(
            @Param("clientId") Long clientId,
            @Param("conversationId") Long conversationId,
            @Param("status") StatusType status,
            @Param("priority") PriorityType priority,
            @Param("channel") ChannelType channel,
            Pageable pageable);


    @Query("""
            select m.conversation.id as conversationId,
                   m.content as content, m.timestamp as timestamp
            from Message m
            where m.conversation.id in :conversationIds
              and m.conversation.client.id = :clientId
              and m.sender.id = :clientId
              and not exists (
                  select newer.id from Message newer
                  where newer.conversation.id = m.conversation.id
                    and newer.sender.id = :clientId
                    and (newer.timestamp > m.timestamp
                         or (newer.timestamp = m.timestamp and newer.id > m.id))
              )
            """)
    List<LatestMessageView> findLatestMessages(
            @Param("clientId") Long clientId,
            @Param("conversationIds") Collection<Long> conversationIds);

    @Query("""
            select m.conversation.id as conversationId, count(m) as unreadCount
            from Message m
            where m.conversation.client.id = :clientId
              and m.sender.id = :clientId
              and m.conversation.id in :conversationIds
              and m.status in :statuses
            group by m.conversation.id
            """)
    List<UnreadCountView> countUnreadMessages(
            @Param("clientId") Long clientId,
            @Param("conversationIds") Collection<Long> conversationIds,
            @Param("statuses") Collection<StatusType> statuses);


    @Query("""
            select m
            from Message m
            where m.nextAttemptAt is not null
              and m.nextAttemptAt <= :now
              and m.status = :status
            order by m.nextAttemptAt, m.id
            """)
    List<Message> findAllFailedAndNextAttemptEqualOrLessThan(
            @Param("now") Instant now,
            @Param("status") StatusType status,
            Pageable pageable
    );
}