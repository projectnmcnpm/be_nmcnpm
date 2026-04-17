package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.response.BookingResponse;
import com.nmcnpm.Homestay.dto.response.DashboardAdminResponse;
import com.nmcnpm.Homestay.dto.response.DashboardStaffResponse;
import com.nmcnpm.Homestay.enums.BookingStatus;
import com.nmcnpm.Homestay.enums.RoomStatus;
import com.nmcnpm.Homestay.mapper.BookingMapper;
import com.nmcnpm.Homestay.repository.DashboardCustomerRepository;
import com.nmcnpm.Homestay.repository.DashboardRepository;
import com.nmcnpm.Homestay.repository.DashboardRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int RECENT_BOOKINGS_LIMIT    = 10;
    private static final int UPCOMING_ACTIVE_LIMIT    = 10;
    private static final int NEW_CUSTOMER_DAYS        = 30;

    private final DashboardRepository         dashboardRepository;
    private final DashboardRoomRepository     dashboardRoomRepository;
    private final DashboardCustomerRepository dashboardCustomerRepository;
    private final BookingMapper               bookingMapper;

    // -------------------------------------------------------------------------
    // GET /api/dashboard/admin   — role: MANAGER
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public DashboardAdminResponse getAdminDashboard() {

        // --- Doanh thu ---
        BigDecimal totalRevenue = dashboardRepository.sumCompletedRevenue();

        // --- Bookings ---
        long totalBookings = dashboardRepository.countAllBookings();

        // --- Rooms ---
        long availableRooms = dashboardRoomRepository.countByStatus(RoomStatus.AVAILABLE);
        long fewLeft        = dashboardRoomRepository.countByStatus(RoomStatus.FEW_LEFT);
        long full           = dashboardRoomRepository.countByStatus(RoomStatus.FULL);
        long cleaningRooms  = dashboardRoomRepository.countByStatus(RoomStatus.CLEANING);
        long totalRooms     = dashboardRoomRepository.countTotal();

        // activeRooms = few_left + full (phòng đang được sử dụng / không available)
        long activeRooms = fewLeft + full;

        // occupancyRate = activeRooms / total * 100  (tránh chia 0)
        int occupancyRate = totalRooms > 0
                ? (int) Math.round((double) activeRooms / totalRooms * 100)
                : 0;

        // --- New customers (30 ngày gần nhất) ---
        OffsetDateTime since = OffsetDateTime.now().minusDays(NEW_CUSTOMER_DAYS);
        long newCustomers = dashboardCustomerRepository.countNewCustomersSince(since);

        // --- Recent bookings ---
        List<BookingResponse> recentBookings = dashboardRepository
                .findRecentBookings(PageRequest.of(0, RECENT_BOOKINGS_LIMIT))
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());

        return DashboardAdminResponse.builder()
                .totalRevenue(totalRevenue)
                .totalBookings(totalBookings)
                .availableRooms(availableRooms)
                .activeRooms(activeRooms)
                .cleaningRooms(cleaningRooms)
                .occupancyRate(occupancyRate)
                .newCustomers(newCustomers)
                .recentBookings(recentBookings)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/dashboard/staff   — role: MANAGER, RECEPTIONIST
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public DashboardStaffResponse getStaffDashboard() {

        // --- Counts ---
        long upcomingCount  = dashboardRepository.countByStatus(BookingStatus.UPCOMING);
        long activeCount    = dashboardRepository.countByStatus(BookingStatus.ACTIVE);
        long availableRooms = dashboardRoomRepository.countByStatus(RoomStatus.AVAILABLE);
        long cleaningRooms  = dashboardRoomRepository.countByStatus(RoomStatus.CLEANING);

        // --- Danh sách ngắn (tối đa UPCOMING_ACTIVE_LIMIT bản ghi mỗi loại) ---
        PageRequest shortPage = PageRequest.of(0, UPCOMING_ACTIVE_LIMIT);

        List<BookingResponse> upcomingBookings = dashboardRepository
                .findByStatusOrdered(BookingStatus.UPCOMING, shortPage)
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());

        List<BookingResponse> activeBookings = dashboardRepository
                .findByStatusOrdered(BookingStatus.ACTIVE, shortPage)
                .stream()
                .map(bookingMapper::toResponse)
                .collect(Collectors.toList());

        return DashboardStaffResponse.builder()
                .upcomingCount(upcomingCount)
                .activeCount(activeCount)
                .availableRooms(availableRooms)
                .cleaningRooms(cleaningRooms)
                .upcomingBookings(upcomingBookings)
                .activeBookings(activeBookings)
                .build();
    }
}