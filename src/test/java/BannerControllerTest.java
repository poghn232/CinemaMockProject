import com.example.superapp.controller.BannerController;
import com.example.superapp.entity.Banner;
import com.example.superapp.entity.Subscription;
import com.example.superapp.entity.User;
import com.example.superapp.repository.BannerRepository;
import com.example.superapp.service.BannerService;
import com.example.superapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BannerControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private BannerService bannerService;

    @InjectMocks
    private BannerController bannerController;

    @Test
    void shouldReturnAds_whenSubscriptionExpired() {
        // mock userDetails
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("testuser");

        // mock user với subscription hết hạn
        Subscription expiredSub = new Subscription();
        expiredSub.setEndDate(LocalDateTime.now().minusDays(1));

        User mockUser = new User();
        mockUser.setSubscriptions(List.of(expiredSub));

        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // ✅ mock bannerService trả về banner giả
        Banner mockBanner = new Banner();
        mockBanner.setData(new byte[]{1, 2, 3});
        when(bannerService.getBannerById(1)).thenReturn(mockBanner);

        ResponseEntity<byte[]> response = bannerController.retrieveImage(1, mockUserDetails);

        assertNotNull(response);
    }

    @Test
    void shouldReturnAds_whenNoSubs() {
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("testuser");

        User mockUser = new User();
        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // ✅ thêm mock bannerService
        Banner mockBanner = new Banner();
        mockBanner.setData(new byte[]{1, 2, 3});
        when(bannerService.getBannerById(1)).thenReturn(mockBanner);

        ResponseEntity<byte[]> response = bannerController.retrieveImage(1, mockUserDetails);

        assertNotNull(response);
    }

    @Test
    void shouldReturnEmpty_whenSubscriptionValid() {
        // --- ARRANGE ---
        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("testuser");

        Subscription activeSub = new Subscription();
        activeSub.setEndDate(LocalDateTime.now().plusDays(30)); // còn hạn

        User mockUser = new User();
        mockUser.setSubscriptions(List.of(activeSub));

        Banner mockBanner = new Banner();
        mockBanner.setData(new byte[]{1, 2, 3});

        when(userService.getUserByUsername("testuser")).thenReturn(mockUser);

        // --- ACT ---
        ResponseEntity<byte[]> response = bannerController.retrieveImage(1, mockUserDetails);
        // --- ASSERT ---
        assertEquals(response, ResponseEntity.ok(new byte[0]));
    }
}
