// src/main/java/de/saarland/events/repository/EventRepository.java
package de.saarland.events.repository;

import de.saarland.events.dto.CityEventCountDto;
import de.saarland.events.model.EStatus;
import de.saarland.events.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllByEventDateGreaterThanEqualOrderByEventDateAsc(ZonedDateTime date);
    long countByStatus(EStatus status);

    Page<Event> findByStatusOrderByEventDateAsc(EStatus status, Pageable pageable);

    Page<Event> findByCityNameAndStatusIn(String cityName, List<EStatus> statuses, Pageable pageable);

    Page<Event> findByCityNameAndEventDateBeforeAndStatusIn(String cityName, ZonedDateTime date, List<EStatus> statuses, Pageable pageable);
    Page<Event> findByCityNameAndEventDateAfterAndStatusIn(String cityName, ZonedDateTime date, List<EStatus> statuses, Pageable pageable);

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

    @Query("SELECT new de.saarland.events.dto.CityEventCountDto(e.city.name, COUNT(e)) " +
            "FROM Event e " +
            "WHERE e.status IN (de.saarland.events.model.EStatus.APPROVED, de.saarland.events.model.EStatus.REJECTED) " +
            "GROUP BY e.city.name " +
            "ORDER BY e.city.name ASC")
    List<CityEventCountDto> countEventsByCity();

    Page<Event> findByCreatedBy_Id(Long userId, Pageable pageable);

    List<Event> findByIsPremiumTrueAndPremiumUntilBefore(ZonedDateTime now);
}