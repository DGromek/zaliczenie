package edu.iis.mto.testreactor.dishwasher;

import edu.iis.mto.testreactor.dishwasher.engine.Engine;
import edu.iis.mto.testreactor.dishwasher.engine.EngineException;
import edu.iis.mto.testreactor.dishwasher.pump.PumpException;
import edu.iis.mto.testreactor.dishwasher.pump.WaterPump;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static edu.iis.mto.testreactor.dishwasher.DishWasher.MAXIMAL_FILTER_CAPACITY;
import static edu.iis.mto.testreactor.dishwasher.Status.*;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        when(door.closed()).thenReturn(true);
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
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY - 1d);
        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(ERROR_FILTER)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    @Test
    public void properWashingShouldReturnRunResultWithSuccessStatus() {
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);
        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withRunMinutes(unrelevantProgramConfiguration.getProgram()
                                                                                    .getTimeInMinutes())
                                      .withStatus(SUCCESS)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    @Test
    public void engineExceptionShouldReturnRunResultWithErrorProgramStatus() throws EngineException {
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);
        doThrow(EngineException.class).when(engine)
                                      .runProgram(any(WashingProgram.class));

        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(ERROR_PROGRAM)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    @Test
    public void waterPumpExceptionShouldReturnRunResultWithErrorPompStatus() throws PumpException {
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);
        doThrow(PumpException.class).when(waterPump)
                                    .drain();

        RunResult actual = dishWasher.start(unrelevantProgramConfiguration);
        RunResult expected = RunResult.builder()
                                      .withStatus(ERROR_PUMP)
                                      .build();

        MatcherAssert.assertThat(expected, samePropertyValuesAs(actual));
    }

    //Behaviour tests

    @Test
    public void washingWithOtherProgramThanRinseShouldCallEngineAndWaterPumpMethodsTwiceInCorrectOrder()
            throws PumpException, EngineException {
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);
        InOrder callingOrder = inOrder(waterPump, engine);

        dishWasher.start(unrelevantProgramConfiguration);

        //First program
        callingOrder.verify(waterPump)
                    .pour(unrelevantProgramConfiguration.getFillLevel());
        callingOrder.verify(engine)
                    .runProgram(unrelevantProgramConfiguration.getProgram());
        callingOrder.verify(waterPump)
                    .drain();

        //Rinsing program
        callingOrder.verify(waterPump)
                    .pour(unrelevantProgramConfiguration.getFillLevel());
        callingOrder.verify(engine)
                    .runProgram(WashingProgram.RINSE);
        callingOrder.verify(waterPump)
                    .drain();
    }

    @Test
    public void washingShouldCallDoorMethods() {
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);
        InOrder callingOrder = inOrder(door);

        dishWasher.start(unrelevantProgramConfiguration);

        callingOrder.verify(door)
                    .closed();
        callingOrder.verify(door)
                    .lock();
    }

    @Test
    public void washingShouldCallDirtFilterCapacityMethodIfWashingTabletsAreUsed() {
        when(dirtFilter.capacity()).thenReturn(MAXIMAL_FILTER_CAPACITY + 1d);

        dishWasher.start(unrelevantProgramConfiguration);
        verify(dirtFilter).capacity();
    }

    @Test
    public void washingShouldNotCallDirtFilterCapacityMethodIfWashingTabletsAreNotUsed() {
        ProgramConfiguration programConfigurationWithNoTablets = ProgramConfiguration.builder()
                                                                                     .withProgram(WashingProgram.ECO)
                                                                                     .withFillLevel(FillLevel.HALF)
                                                                                     .withTabletsUsed(false)
                                                                                     .build();

        dishWasher.start(programConfigurationWithNoTablets);
        verify(dirtFilter, never()).capacity();
    }
}
