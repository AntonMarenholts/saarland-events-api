// src/main/java/de/saarland/events/repository/EventRepository.java
package de.saarland.events.repository;

import de.saarland.events.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import de.saarland.events.model.EStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.ZonedDateTime; // ИЗМЕНЕНО
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllByEventDateGreaterThanEqualOrderByEventDateAsc(ZonedDateTime date); // ИЗМЕНЕНО
    long countByStatus(EStatus status);
    List<Event> findByStatusOrderByEventDateAsc(EStatus status);

    // V-- ДОБАВЛЕНА СОРТИРОВКА OrderByEventDateDesc --V
    List<Event> findByCityNameAndStatusInOrderByEventDateDesc(String cityName, List<EStatus> statuses);

    @Query("SELECT COUNT(e) > 0 FROM Event e JOIN e.translations t " +
            "WHERE t.locale = 'de' " +
            "AND lower(t.name) = lower(:name) " +
            "AND e.city.id = :cityId " +
            "AND CAST(e.eventDate AS date) = :eventDate")
    boolean existsDuplicate(
            @Param("name") String name,
            @Param("cityId") Long cityId,
            @Param("eventDate") LocalDate eventDate
    );
}