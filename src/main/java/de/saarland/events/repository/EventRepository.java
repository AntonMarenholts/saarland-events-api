package de.saarland.events.repository;

import de.saarland.events.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import de.saarland.events.model.EStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import de.saarland.events.dto.CityEventCountDto;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    List<Event> findAllByEventDateGreaterThanEqualOrderByEventDateAsc(ZonedDateTime date);
    long countByStatus(EStatus status);


    Page<Event> findByStatusOrderByEventDateAsc(EStatus status, Pageable pageable);

    Page<Event> findByCityNameAndStatusIn(String cityName, List<EStatus> statuses, Pageable pageable);

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
}