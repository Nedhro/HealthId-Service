package org.sharedhealth.healthId.web.controller;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sharedhealth.healthId.web.Model.FacilityResponse;
import org.sharedhealth.healthId.web.Model.GeneratedHIDBlock;
import org.sharedhealth.healthId.web.config.HealthIdProperties;
import org.sharedhealth.healthId.web.exception.InvalidRequestException;
import org.sharedhealth.healthId.web.security.UserInfo;
import org.sharedhealth.healthId.web.security.UserProfile;
import org.sharedhealth.healthId.web.service.FacilityService;
import org.sharedhealth.healthId.web.service.HealthIdService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;

import static com.datastax.driver.core.utils.UUIDs.timeBased;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@RunWith(MockitoJUnitRunner.class)
public class HealthIdControllerTest {
    @Mock
    private HealthIdService healthIdService;
    @Mock
    private FacilityService facilityService;
    @Mock
    private HealthIdProperties healthIdProperties;

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();


    @Before
    public void setUp() throws Exception {
        initMocks(this);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(getUserInfo(), null));
    }

    private UserInfo getUserInfo() {
        UserProfile userProfile = new UserProfile("facility", "100067", null);

        return new UserInfo("102", "ABC", "abc@mail", 1, true, "111100",
                new ArrayList<String>(), asList(userProfile));
    }

    @Test
    public void testGenerate() {
        GeneratedHIDBlock hidBlock = new GeneratedHIDBlock(1000L, "MCI", 1000L, 1099L, 100L, "", timeBased());
        when(healthIdService.generateAll(any(UserInfo.class))).thenReturn(hidBlock);
        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, healthIdProperties);
        assertEquals("Generated 100 HIDs.", healthIdController.generate().getResult());
        verify(healthIdService, times(1)).generateAll(any(UserInfo.class));
    }

    @Test
    public void testGenerateRange() {
        long start = 1000L, total = 100L;
        GeneratedHIDBlock hidBlock = new GeneratedHIDBlock(1000L, "MCI", 1000L, 1099L, 100L, "", timeBased());
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setMciStartHid("1000");
        testProperties.setMciEndHid("3000");

        when(healthIdService.generateBlock(eq(start), eq(total), any(UserInfo.class))).thenReturn(hidBlock);
        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        ResponseEntity result = (ResponseEntity) healthIdController.generateBlock(start, total).getResult();
        assertEquals("Generated 100 HIDs.", result.getBody());
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(healthIdService, times(1)).generateBlock(eq(start), eq(total), any(UserInfo.class));
    }

    @Test
    public void shouldGenerateBlockForOrg() throws Exception {
        long start = 1000L, total = 100L;
        String facilityID = "12345";
        FacilityResponse facility = new FacilityResponse();
        facility.setId(facilityID);
        facility.setName("ABC");
        GeneratedHIDBlock hidBlock = new GeneratedHIDBlock(1000L, facilityID, 1000L, 1099L, 100L, "", timeBased());
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setOtherOrgStartHid("1000");
        testProperties.setOtherOrgEndHid("3000");
        when(facilityService.find(facilityID)).thenReturn(facility);
        when(healthIdService.generateBlockForOrg(eq(start), eq(total), eq(facilityID), any(UserInfo.class))).thenReturn(hidBlock);

        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        ResponseEntity result = (ResponseEntity) healthIdController.generateBlockForOrg(facilityID, start, total).getResult();

        assertEquals("Generated 100 HIDs.", result.getBody());
        verify(healthIdService, times(1)).generateBlockForOrg(eq(start), eq(total), eq(facilityID), any(UserInfo.class));
    }

    @Test
    public void shouldGenerateBlockForOrgWhenSeriesIsExhausted() throws Exception {
        long start = 1000L, total = 150L;
        String facilityID = "12345";
        FacilityResponse facility = new FacilityResponse();
        facility.setId(facilityID);
        facility.setName("ABC");
        GeneratedHIDBlock hidBlock = new GeneratedHIDBlock(1000L, facilityID, 1000L, 1099L, 100L, "", timeBased());
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setOtherOrgStartHid("1000");
        testProperties.setOtherOrgEndHid("3000");


        when(facilityService.find(facilityID)).thenReturn(facility);
        when(healthIdService.generateBlockForOrg(eq(start), eq(total), eq(facilityID), any(UserInfo.class))).thenReturn(hidBlock);

        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        ResponseEntity result = (ResponseEntity) healthIdController.generateBlockForOrg(facilityID, start, total).getResult();

        assertEquals("Can generate only 100 HIDs, because series exhausted. Use another series.", result.getBody());
        verify(healthIdService, times(1)).generateBlockForOrg(eq(start), eq(total), eq(facilityID), any(UserInfo.class));
    }

    @Test
    public void shouldNotGenerateBlockForOrgWhenGivenOrganizationIsInvalid() throws Exception {
        long start = 1000L, total = 150L;

        expectedEx.expect(InvalidRequestException.class);
        expectedEx.expectMessage("Invalid Organization:- 12345");

        String facilityId = "12345";
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setOtherOrgStartHid("1000");
        testProperties.setOtherOrgEndHid("3000");
        when(facilityService.find(facilityId)).thenReturn(null);

        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        healthIdController.generateBlockForOrg(facilityId, start, total);
        verify(healthIdService, never()).generateBlockForOrg(anyLong(), anyLong(), anyString(), any(UserInfo.class));
    }

    @Test
    public void shouldNotGenerateBlockWhenTotalHIDsAreMoreThanTwoMillion() throws Exception {
        expectedEx.expect(InvalidRequestException.class);
        expectedEx.expectMessage("Total HIDs should not be more than 2000000");

        long start = 1000L, total = 2000001L;
        String facilityID = "12345";
        FacilityResponse facility = new FacilityResponse();
        facility.setId(facilityID);
        facility.setName("ABC");
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setOtherOrgStartHid("1000");
        testProperties.setOtherOrgEndHid("3000");
        when(facilityService.find(facilityID)).thenReturn(facility);

        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        healthIdController.generateBlockForOrg(facilityID, start, total);

        verify(healthIdService, never()).generateBlockForOrg(anyLong(), anyLong(), anyString(), any(UserInfo.class));
    }

    @Test
    public void shouldNotGenerateBlockWhenInvalidStartForMci() throws Exception {
        long start = 1000L, total = 2000001L;
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setMciStartHid("2000");
        testProperties.setMciEndHid("3000");

        expectedEx.expect(InvalidRequestException.class);
        expectedEx.expectMessage("1000 not for MCI");

        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        healthIdController.generateBlock(start, total);

        verify(healthIdService, never()).generateBlock(anyLong(), anyLong(), any(UserInfo.class));
    }

    @Test
    public void shouldNotGenerateBlockForOrgIfOrgIsMci() throws Exception {
        long start = 2000L, total = 20L;
        String mciFacilityId = "1234";
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setMciOrgCode("1234");
        expectedEx.expect(InvalidRequestException.class);
        expectedEx.expectMessage("This endpoint is not for MCI. To generate HIDs for MCI use /generateBlock endpoint");

        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        healthIdController.generateBlockForOrg(mciFacilityId, start, total);
        verify(healthIdService, never()).generateBlockForOrg(eq(start), eq(total), eq(mciFacilityId), any(UserInfo.class));
    }

    @Test
    public void shouldNotGenerateBlockWhenInvalidStartForOrg() throws Exception {
        long start = 4000L, total = 20L;
        HealthIdProperties testProperties = new HealthIdProperties();
        testProperties.setOtherOrgStartHid("2000");
        testProperties.setOtherOrgEndHid("3000");
        String facilityID = "12345";
        FacilityResponse facility = new FacilityResponse();
        facility.setId(facilityID);
        facility.setName("ABC");

        when(facilityService.find(facilityID)).thenReturn(facility);

        expectedEx.expect(InvalidRequestException.class);
        expectedEx.expectMessage("4000 series is not valid.");

        HealthIdController healthIdController = new HealthIdController(healthIdService, facilityService, testProperties);
        healthIdController.generateBlockForOrg(facilityID, start, total);

        verify(healthIdService, never()).generateBlockForOrg(anyLong(), anyLong(), anyString(), any(UserInfo.class));
    }
}