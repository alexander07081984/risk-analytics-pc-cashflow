package org.pillarone.riskanalytics.domain.pc.cf.pattern;

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
public class PayoutPattern extends Pattern implements IPayoutPatternMarker {
    @Override
    protected Class<? extends IPatternMarker> getPatternMarker() {
        return IPayoutPatternMarker.class;
    }
}
