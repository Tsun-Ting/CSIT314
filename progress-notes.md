# TravelApp Progress Notes

## Project Path
- `/Users/gaurabgurung/Desktop/TravelApp/TravelApp`

## Current Stack
- Spring Boot
- Thymeleaf
- Spring Data JPA
- H2 Database
- Validation
- Spring Security dependency is installed
- A `SecurityConfig` class exists so development can continue without being blocked by the default login page

## UML Alignment Status
- Implemented and in use:
  - `User`
  - `Trip`
  - `Itinerary`
  - `Activity`
  - `Budget`
  - `Expense`
  - `Bookings`
  - `FlightBooking`
  - `HotelBooking`
  - `Recommendation`
  - `SharedItinerary`
  - `Notification`
- Still missing from the class diagram:
  - None
- Notes:
  - The code follows the UML classes and relationships, but service logic has been placed in Spring services/controllers instead of entity methods like `register()` or `createTrip()`

## Model Classes Implemented
- `User`
- `Trip`
- `Itinerary`
- `Activity`
- `Budget`
- `Expense`
- `Bookings`
- `FlightBooking`
- `HotelBooking`
- `Recommendation`
- `Notification`

## Repositories Implemented
- `UserRepository`
- `TripRepository`
- `ItineraryRepository`
- `ActivityRepository`
- `BudgetRepository`
- `ExpenseRepository`
- `BookingsRepository`
- `RecommendationRepository`
- `SharedItineraryRepository`
- `NotificationRepository`

## Services Implemented
- `TripService`
- `ActivityService`
- `BudgetService`
- `BookingService`
- `RecommendationService`
- `SharedItineraryService`
- `NotificationService`

## Controllers Implemented
- `HomeController`
- `TripController`
- `ActivityController`
- `BudgetController`
- `BookingController`
- `RecommendationController`
- `SharedItineraryController`

## Templates Implemented
- `index.html`
- `trips.html`
- `trip-details.html`
- `shared-itinerary.html`
- `shared-itinerary-view.html`

## Working Pages and Routes
- `/` dashboard
- `/trips` trip management page
- `/trips/{id}` trip details page
- `POST /trips`
- `POST /activities`
- `POST /expenses`
- `POST /bookings`
- `POST /recommendations/add-to-itinerary`
- `GET /shared-itinerary`
- `POST /shared-itinerary`
- `GET /shared-itinerary/{shareToken}`

## Working Features
- Create a trip
- Seed a sample trip if the database is empty
- View the trip list
- Open trip details
- Add activity to a trip itinerary
- View activities for a trip
- Add expense to a trip budget
- View spent and remaining budget
- View expense list
- Simulate flight and hotel bookings
- View saved bookings on the trip page
- Seed recommendations for the sample trip
- Add a recommendation into the itinerary as an activity
- Create token-based shared itinerary links for trips
- Open a shared itinerary page using a public token
- Generate notifications for core trip actions
- View recent notifications on the dashboard and trip page

## Important Implementation Notes
- `Trip` is the center of the current UI flow, which matches the UML better than putting everything on the homepage
- The trip details page currently contains the trip-specific features:
  - activities
  - expenses/budget
  - bookings
  - recommendations
- Recommendation rendering was changed to use a dedicated model attribute instead of `trip.recommendations` directly
- This was done to avoid lazy-loading issues during startup and view rendering

## Known Gaps
- Homepage still has placeholder-style navigation and is not polished
- There are no dedicated pages yet for:
  - profile
  - bookings
  - recommendations
- User registration/login/profile flow is not implemented yet
- Dedicated notification controls such as mark-as-read are not implemented

## Next Recommended Tasks
- Add user/profile flow if needed for `FR-1`
- Polish UI after the remaining functional pieces are in place

## Verification
- Spring context loads successfully after the recommendation fix
- Surefire report shows:
  - `tests="1"`
  - `errors="0"`
  - `failures="0"`
- Report file:
  - `/Users/gaurabgurung/Desktop/TravelApp/TravelApp/target/surefire-reports/TEST-com.example.TravelApp.TravelAppApplicationTests.xml`

## Resume Prompt For New Chat
Use this if context gets too full:

`Spring Boot TravelApp in /Users/gaurabgurung/Desktop/TravelApp/TravelApp. Please read /Users/gaurabgurung/Desktop/TravelApp/TravelApp/progress-notes.md first. Current state: trips, activities, expenses, bookings, recommendations, SharedItinerary, and Notification are working. Next task: polish the UI/navigation further or add user/profile flow if needed.`
