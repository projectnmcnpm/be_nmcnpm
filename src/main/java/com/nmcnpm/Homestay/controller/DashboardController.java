package com.nmcnpm.Homestay.controller;

import com.nmcnpm.Homestay.dto.response.ApiResponse;
import com.nmcnpm.Homestay.dto.response.DashboardAdminResponse;
import com.nmcnpm.Homestay.dto.response.DashboardStaffResponse;
import com.nmcnpm.Homestay.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard endpoints:
 *
 *   GET /api/dashboard/admin   — MANAGER only
 *   GET /api/dashboard/staff   — MANAGER + RECEPTIONIST
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ------------------------------------------------------------------
    // GET /api/dashboard/admin
    // Response: totalRevenue, totalBookings, availableRooms, activeRooms,
    //           cleaningRooms, occupancyRate, newCustomers, recentBookings[]
    // ------------------------------------------------------------------
    @GetMapping("/admin")
    @PreAuthorize("hasRole('MANAGER')")
    public ApiResponse<DashboardAdminResponse> getAdminDashboard() {
        return ApiResponse.success(dashboardService.getAdminDashboard());
    }

    // ------------------------------------------------------------------
    // GET /api/dashboard/staff
    // Response: upcomingCount, activeCount, availableRooms, cleaningRooms,
    //           upcomingBookings[], activeBookings[]
    // ------------------------------------------------------------------
    @GetMapping("/staff")
    @PreAuthorize("hasAnyRole('MANAGER', 'RECEPTIONIST')")
    public ApiResponse<DashboardStaffResponse> getStaffDashboard() {
        return ApiResponse.success(dashboardService.getStaffDashboard());
    }
}