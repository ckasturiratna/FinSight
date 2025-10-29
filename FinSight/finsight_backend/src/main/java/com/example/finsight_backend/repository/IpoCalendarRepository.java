package com.example.finsight_backend.repository;

import com.example.finsight_backend.entity.IpoCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IpoCalendarRepository extends JpaRepository<IpoCalendar, Long> {
    
    /**
     * Find IPO by symbol
     */
    Optional<IpoCalendar> findBySymbol(String symbol);
    
    /**
     * Check if IPO exists by symbol
     */
    boolean existsBySymbol(String symbol);
    
    /**
     * Find IPOs by date range
     */
    List<IpoCalendar> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find IPOs for today and tomorrow
     */
    @Query("SELECT i FROM IpoCalendar i WHERE i.date BETWEEN :today AND :tomorrow ORDER BY i.date ASC")
    List<IpoCalendar> findUpcomingIpos(@Param("today") LocalDate today, @Param("tomorrow") LocalDate tomorrow);
    
    /**
     * Find IPOs by status
     */
    List<IpoCalendar> findByStatus(String status);
    
    /**
     * Find IPOs by exchange
     */
    List<IpoCalendar> findByExchange(String exchange);
}
