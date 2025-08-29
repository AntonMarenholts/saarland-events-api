
package de.saarland.events.specification;

import de.saarland.events.model.EStatus;
import de.saarland.events.model.Event;
import de.saarland.events.model.Translation;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class EventSpecification {

    public Specification<Event> findByCriteria(
            Optional<String> cityName,
            Optional<Long> categoryId,
            Optional<Integer> year,
            Optional<Integer> month,
            Optional<String> categoryName,
            Optional<String> keyword
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("status"), EStatus.APPROVED));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), ZonedDateTime.now()));

            cityName.ifPresent(c -> predicates.add(criteriaBuilder.equal(root.get("city").get("name"), c)));
            categoryId.ifPresent(id -> predicates.add(criteriaBuilder.equal(root.get("category").get("id"), id)));
            categoryName.ifPresent(name -> predicates.add(criteriaBuilder.equal(root.get("category").get("name"), name)));

            keyword.ifPresent(kw -> {
                Join<Event, Translation> translationJoin = root.join("translations");
                Predicate namePredicate = criteriaBuilder.like(criteriaBuilder.lower(translationJoin.get("name")), "%" + kw.toLowerCase() + "%");
                Predicate descPredicate = criteriaBuilder.like(criteriaBuilder.lower(translationJoin.get("description")), "%" + kw.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(namePredicate, descPredicate));
            });

            if (year.isPresent() && month.isPresent()) {
                ZonedDateTime startDate = ZonedDateTime.of(year.get(), month.get(), 1, 0, 0, 0, 0, ZoneId.systemDefault());
                ZonedDateTime endDate = startDate.plusMonths(1);
                predicates.add(criteriaBuilder.between(root.get("eventDate"), startDate, endDate));
            } else if (year.isPresent()) {
                ZonedDateTime startDate = ZonedDateTime.of(year.get(), 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
                ZonedDateTime endDate = startDate.plusYears(1);
                predicates.add(criteriaBuilder.between(root.get("eventDate"), startDate, endDate));
            } else if (month.isPresent()) {
                int currentYear = LocalDateTime.now().getYear();
                ZonedDateTime startDate = ZonedDateTime.of(currentYear, month.get(), 1, 0, 0, 0, 0, ZoneId.systemDefault());
                ZonedDateTime endDate = startDate.plusMonths(1);
                predicates.add(criteriaBuilder.between(root.get("eventDate"), startDate, endDate));
            }

            query.distinct(true);
            // СНАЧАЛА ПРЕМИУМ, ПОТОМ ПО ДАТЕ
            query.orderBy(criteriaBuilder.desc(root.get("isPremium")), criteriaBuilder.asc(root.get("eventDate")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
