package controllers.nanonis.records;

public record TipShaperProps(
        float switchOffDelay,
        boolean changeBias,
        float bias,
        float tipLift,
        float liftTime1,
        float biasLift,
        float biasSettleTime,
        float liftHeight,
        float liftTime2,
        float endWaitTime,
        boolean restoreFeedback) {

}
