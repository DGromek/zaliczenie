package edu.iis.mto.testreactor.dishwasher;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.iis.mto.testreactor.dishwasher.Status.DOOR_OPEN;
import static edu.iis.mto.testreactor.dishwasher.Status.SUCCESS;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishWasherTest {

    @Mock
    private WaterPump waterPump;
    @Mock
    private Engine engine;
    @Mock
    private DirtFilter dirtFilter;
    @Mock
    private Door door;
    private DishWasher dishWasher;

    @BeforeEach
    public void setUp() {
        dishWasher = new DishWasher(waterPump, engine, dirtFilter, door);
    }

    //State tests
    @Test
    public void washingWithOpenedDoorShouldReturnRunResultWithDoorOpen() {
        when(door.closed()).thenReturn(false);
        ProgramConfiguration programConfiguration = ProgramConfiguration.builder()
                                                   .withProgram(WashingProgram.ECO)
                                                   .withFillLevel(FillLevel.HALF)
                                                   .withTabletsUsed(true)
                                                   .build();

        RunResult actual = dishWasher.start(programConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(DOOR_OPEN)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

}
