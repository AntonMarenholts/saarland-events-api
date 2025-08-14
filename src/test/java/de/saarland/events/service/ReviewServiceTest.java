package de.saarland.events.service;

import de.saarland.events.dto.ReviewRequestDto;
import de.saarland.events.model.Event;
import de.saarland.events.model.Review;
import de.saarland.events.model.User;
import de.saarland.events.repository.EventRepository;
import de.saarland.events.repository.ReviewRepository;
import de.saarland.events.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {


    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;


    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Event pastEvent;
    private Event futureEvent;
    private ReviewRequestDto reviewRequestDto;


    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);

        pastEvent = new Event();
        pastEvent.setId(1L);
        pastEvent.setEventDate(LocalDateTime.now().minusDays(1));

        futureEvent = new Event();
        futureEvent.setId(2L);
        futureEvent.setEventDate(LocalDateTime.now().plusDays(1));

        reviewRequestDto = new ReviewRequestDto();
        reviewRequestDto.setRating(5);
        reviewRequestDto.setComment("Great concert!");
    }

    @Test
    void createReview_Success() {

        when(eventRepository.findById(pastEvent.getId())).thenReturn(Optional.of(pastEvent));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(reviewRepository.findByEventIdAndUserId(pastEvent.getId(), testUser.getId())).thenReturn(Optional.empty()); // Пользователь еще не оставлял отзыв
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));


        Review createdReview = reviewService.createReview(pastEvent.getId(), testUser.getId(), reviewRequestDto);


        assertNotNull(createdReview);
        assertEquals(5, createdReview.getRating());
        assertEquals(testUser, createdReview.getUser());
        assertEquals(pastEvent, createdReview.getEvent());


        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    void createReview_ThrowsException_WhenEventIsInFuture() {

        when(eventRepository.findById(futureEvent.getId())).thenReturn(Optional.of(futureEvent));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(futureEvent.getId(), testUser.getId(), reviewRequestDto)
        );


        assertEquals("You can only review past events.", exception.getMessage());


        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void createReview_ThrowsException_WhenUserAlreadyReviewed() {

        when(eventRepository.findById(pastEvent.getId())).thenReturn(Optional.of(pastEvent));
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));

        when(reviewRepository.findByEventIdAndUserId(pastEvent.getId(), testUser.getId())).thenReturn(Optional.of(new Review()));


        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> reviewService.createReview(pastEvent.getId(), testUser.getId(), reviewRequestDto)
        );

        assertEquals("User has already reviewed this event.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }
}