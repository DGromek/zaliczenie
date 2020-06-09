package edu.iis.mto.testreactor.dishwasher;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.iis.mto.testreactor.dishwasher.DishWasher.MAXIMAL_FILTER_CAPACITY;
import static edu.iis.mto.testreactor.dishwasher.Status.*;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
    private ProgramConfiguration unrelevantProgramConfiguration;

    @BeforeEach
    public void setUp() {
        dishWasher = new DishWasher(waterPump, engine, dirtFilter, door);
        unrelevantProgramConfiguration = ProgramConfiguration.builder()
                                                             .withProgram(WashingProgram.ECO)
                                                             .withFillLevel(FillLevel.HALF)
                                                             .withTabletsUsed(true)
                                                             .build();
    }

    //State tests
    @Test
    public void washingWithOpenedDoorShouldReturnRunResultWithDoorOpen() {
        when(door.closed()).thenReturn(false);

        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(DOOR_OPEN)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    @Test
    public void washingWithDirtFilterCapacityAboveMaxShouldReturnRunResultWithErrorFilterStatus() {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY - 1d);

        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(ERROR_FILTER)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    @Test
    public void properWashingShouldReturnRunResultWithSuccessStatus() {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);

        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withRunMinutes(unrelevantProgramConfiguration.getProgram().getTimeInMinutes())
                                      .withStatus(SUCCESS)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    @Test
    public void engineExceptionShouldReturnRunResultWithErrorProgramStatus() throws EngineException {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);
        doThrow(EngineException.class).when(engine).runProgram(any(WashingProgram.class));

        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(ERROR_PROGRAM)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    @Test
    public void waterPumpExceptionShouldReturnRunResultWithErrorPompStatus() throws PumpException {
        when(door.closed()).thenReturn(true);
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);
        doThrow(PumpException.class).when(waterPump).drain();

        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(ERROR_PUMP)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }
}
