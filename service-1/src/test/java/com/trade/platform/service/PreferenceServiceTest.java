package com.trade.platform.service;

import com.trade.platform.dto.PreferenceRequest;
import com.trade.platform.dto.PreferenceResponse;
import com.trade.platform.entity.CustomerPreference;
import com.trade.platform.mapper.PreferenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PreferenceServiceTest {

    @Mock
    private PreferenceMapper preferenceMapper;

    @InjectMocks
    private PreferenceService preferenceService;

    @Test
    void createsBlankPreferenceWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(preferenceMapper.findByUserId(userId)).thenReturn(null);

        PreferenceResponse response = preferenceService.get(userId);

        assertThat(response.completed()).isFalse();
        verify(preferenceMapper).insert(any(CustomerPreference.class));
    }

    @Test
    void returnsExistingPreferences() {
        UUID userId = UUID.randomUUID();
        CustomerPreference prefs = new CustomerPreference();
        prefs.setIsCompleted(true);
        prefs.setTradingExperience("BEGINNER");
        prefs.setRiskTolerance("MODERATE");
        prefs.setPreferredSectors("TECHNOLOGY,FINANCE");
        when(preferenceMapper.findByUserId(userId)).thenReturn(prefs);

        PreferenceResponse response = preferenceService.get(userId);

        assertThat(response.completed()).isTrue();
        assertThat(response.preferredSectors()).containsExactly("TECHNOLOGY", "FINANCE");
    }

    @Test
    void savesPreferencesAndMarksCompleted() {
        UUID userId = UUID.randomUUID();
        when(preferenceMapper.findByUserId(userId)).thenReturn(null, preferenceOf(userId));
        PreferenceRequest request = new PreferenceRequest(
                "INTERMEDIATE", "AGGRESSIVE", "SWING_TRADING", "MEDIUM_TERM",
                List.of("TECHNOLOGY"), "WEEKLY");

        PreferenceResponse response = preferenceService.save(userId, request);

        ArgumentCaptor<CustomerPreference> captor = ArgumentCaptor.forClass(CustomerPreference.class);
        verify(preferenceMapper).update(captor.capture());
        assertThat(captor.getValue().getTradingExperience()).isEqualTo("INTERMEDIATE");
        assertThat(captor.getValue().getPreferredSectors()).isEqualTo("TECHNOLOGY");
        assertThat(response.completed()).isTrue();

        ArgumentCaptor<CustomerPreference> insertCaptor = ArgumentCaptor.forClass(CustomerPreference.class);
        verify(preferenceMapper).insert(insertCaptor.capture());
        assertThat(insertCaptor.getValue().getUserId()).isEqualTo(userId);
    }

    private CustomerPreference preferenceOf(UUID userId) {
        CustomerPreference p = new CustomerPreference();
        p.setUserId(userId);
        p.setIsCompleted(true);
        return p;
    }
}