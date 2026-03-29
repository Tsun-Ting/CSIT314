package com.example.TravelApp.service;

import com.example.TravelApp.model.Budget;
import com.example.TravelApp.model.Expense;
import com.example.TravelApp.model.Trip;
import com.example.TravelApp.repository.BudgetRepository;
import com.example.TravelApp.repository.ExpenseRepository;
import com.example.TravelApp.repository.TripRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class BudgetService {

    private final TripRepository tripRepository;
    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final NotificationService notificationService;

    public BudgetService(TripRepository tripRepository,
                         BudgetRepository budgetRepository,
                         ExpenseRepository expenseRepository,
                         NotificationService notificationService) {
        this.tripRepository = tripRepository;
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.notificationService = notificationService;
    }

    public void addExpenseToTrip(Long tripId,
                                 String ownerEmail,
                                 String category,
                                 Double amount,
                                 LocalDate date,
                                 String description) {
        Trip trip = tripRepository.findByIdAndUserEmail(tripId, ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found for this user"));

        Budget budget = trip.getBudget();
        if (budget == null) {
            Double totalBudget = trip.getTotalBudget() != null ? trip.getTotalBudget() : 0.0;
            budget = new Budget(totalBudget, 0.0, totalBudget);
            trip.setBudget(budget);
        }

        Expense expense = new Expense(category, amount, date, description);
        budget.addExpense(expense);

        double currentSpent = budget.getSpentAmount() != null ? budget.getSpentAmount() : 0.0;
        double totalBudget = budget.getTotalBudget() != null ? budget.getTotalBudget() : 0.0;
        double newSpent = currentSpent + amount;

        budget.setSpentAmount(newSpent);
        budget.setRemainingAmount(totalBudget - newSpent);

        tripRepository.save(trip);
        budgetRepository.save(budget);
        expenseRepository.save(expense);
        notificationService.createNotification(
                trip,
                "EXPENSE",
                "Added expense of $" + amount + " for " + category + "."
        );
    }
}
