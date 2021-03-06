package org.pillarone.riskanalytics.domain.pc.cf.exposure;

import org.pillarone.riskanalytics.domain.utils.marker.IReinsuranceContractMarker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author stefan.kunz (at) intuitive-collaboration (dot) com
 */
public class CededUnderwritingInfoPacket extends UnderwritingInfoPacket {

    private double premiumPaidFixed;
    private double premiumPaidVariable;

    private double commission;
    private double commissionFixed;
    private double commissionVariable;

    private UnderwritingInfoPacket original;

    public CededUnderwritingInfoPacket() {
        super();
    }

    /**
     *
     * @param policyFactor
     * @param premiumFactor
     * @param commissionFactor
     * @return creates a cloned instance with numberOfPolicies and premiumWritten according to parameters
     */
    public CededUnderwritingInfoPacket withFactorsApplied(double policyFactor, double premiumFactor, double commissionFactor) {
        return withFactorsApplied(policyFactor, premiumFactor, premiumFactor, commissionFactor);
    }

    /**
     *
     * @param policyFactor
     * @param premiumWrittenFactor
     * @param premiumPaidFactor
     * @param commissionFactor
     * @return creates a cloned instance with numberOfPolicies and premiumWritten according to parameters
     */
    public CededUnderwritingInfoPacket withFactorsApplied(double policyFactor, double premiumWrittenFactor,
                                                          double premiumPaidFactor, double commissionFactor) {
        CededUnderwritingInfoPacket modified = (CededUnderwritingInfoPacket) this.clone();
        modified.numberOfPolicies *= policyFactor;
        modified.premiumWritten *= premiumWrittenFactor;
        modified.premiumPaid *= premiumPaidFactor;
        modified.premiumPaidFixed *= premiumPaidFactor;
        modified.premiumPaidVariable *= premiumPaidFactor;
        modified.commission *= commissionFactor;
        modified.commissionFixed *= commissionFactor;
        modified.commissionVariable *= commissionFactor;
        return modified;
    }

    /**
     * @param policyFactor
     * @param premiumFactor
     * @return creates a cloned instance with numberOfPolicies and premiumWritten according to parameters
     */
    public UnderwritingInfoPacket withFactorsApplied(double policyFactor, double premiumFactor) {
        UnderwritingInfoPacket modified = new UnderwritingInfoPacket(this);
        modified.numberOfPolicies *= policyFactor;
        modified.premiumWritten *= premiumFactor;
        modified.premiumPaid *= premiumFactor;
        return modified;
    }

    public static CededUnderwritingInfoPacket deriveCededPacket(UnderwritingInfoPacket packet,
                                                                IReinsuranceContractMarker contract) {
        CededUnderwritingInfoPacket cededPacket = new CededUnderwritingInfoPacket();
        cededPacket.premiumWritten = -packet.premiumWritten;
        cededPacket.premiumPaid = -packet.premiumPaid;
        cededPacket.premiumPaidFixed = -packet.premiumPaid;
        cededPacket.numberOfPolicies = packet.numberOfPolicies;
        cededPacket.sumInsured = packet.sumInsured;
        cededPacket.maxSumInsured = packet.maxSumInsured;
        cededPacket.exposure = packet.exposure;
        cededPacket.original = packet.original;
        cededPacket.segment = packet.segment;
        cededPacket.reinsuranceContract = contract;
        cededPacket.setDate(packet.getDate());
        return cededPacket;
    }

    public static CededUnderwritingInfoPacket deriveCededPacketForNonPropContract(UnderwritingInfoPacket packet,
                 IReinsuranceContractMarker contract, double premium, double premiumFixed, double premiumVariable) {
        CededUnderwritingInfoPacket cededPacket = new CededUnderwritingInfoPacket();
        cededPacket.premiumWritten = premium == -0 ? 0 : premium;
        cededPacket.premiumPaid = cededPacket.premiumWritten;
        cededPacket.premiumPaidFixed = premiumFixed == -0 ? 0 : premiumFixed;
        cededPacket.premiumPaidVariable = premiumVariable == -0 ? 0 : premiumVariable;
        cededPacket.numberOfPolicies = packet.numberOfPolicies;
        cededPacket.sumInsured = 0;
        cededPacket.maxSumInsured = 0;
        cededPacket.exposure = packet.exposure;
        cededPacket.original = packet.original;
        cededPacket.setRiskBand(packet.riskBand());
        cededPacket.segment = packet.segment;
        cededPacket.reinsuranceContract = contract;
        cededPacket.setDate(packet.getDate());
        return cededPacket;
    }

    public static CededUnderwritingInfoPacket scale(UnderwritingInfoPacket packet, IReinsuranceContractMarker contract,
                                                    double policyFactor, double premiumFactor, double commissionFactor) {
        CededUnderwritingInfoPacket cededPacket = deriveCededPacket(packet, contract);
        return cededPacket.withFactorsApplied(policyFactor, premiumFactor, commissionFactor);
    }

    public static CededUnderwritingInfoPacket scale(UnderwritingInfoPacket packet, IReinsuranceContractMarker contract,
                                                    double policyFactor, double premiumWrittenFactor,
                                                    double premiumPaidFactor, double commissionFactor) {
        CededUnderwritingInfoPacket cededPacket = deriveCededPacket(packet, contract);
        return cededPacket.withFactorsApplied(policyFactor, premiumWrittenFactor, premiumPaidFactor, commissionFactor);
    }

    public void setCommission(double fixed, double variable) {
        commission = fixed + variable;
        commissionFixed = fixed;
        commissionVariable = variable;
    }

    /**
     * Adds additive UnderwritingInfo fields (premium) as well as combining ExposureInfo fields.
     * averageSumInsured is not adjusted!
     *
     * @param other
     * @return UnderwritingInfo packet with resulting fields
     */
    public UnderwritingInfoPacket plus(UnderwritingInfoPacket other) {
        super.plus(other);
        if (other instanceof CededUnderwritingInfoPacket) {
            premiumPaidFixed += ((CededUnderwritingInfoPacket) other).premiumPaidFixed;
            premiumPaidVariable += ((CededUnderwritingInfoPacket) other).premiumPaidVariable;
            commission += ((CededUnderwritingInfoPacket) other).commission;
            commissionFixed += ((CededUnderwritingInfoPacket) other).commissionFixed;
            commissionVariable += ((CededUnderwritingInfoPacket) other).commissionVariable;
        }
        return this;
    }

    public CededUnderwritingInfoPacket plus(CededUnderwritingInfoPacket other) {
        return (CededUnderwritingInfoPacket) plus((UnderwritingInfoPacket) other);
    }

    public void adjustCommissionProperties(double commissionFactor, double fixedCommissionFactor,
                                           double variableCommissionFactor, boolean isAdditive) {
        if (isAdditive) {
            adjustCommissionProperties(commissionFactor, fixedCommissionFactor, variableCommissionFactor);
        }
        else {
            setCommissionProperties(commissionFactor, fixedCommissionFactor, variableCommissionFactor);
        }
    }

    public void setCommissionProperties(double commissionFactor, double fixedCommissionFactor, double variableCommissionFactor) {
        this.commission = -premiumPaid * commissionFactor;
        commissionFixed = -premiumPaid * fixedCommissionFactor;
        commissionVariable = -premiumPaid * variableCommissionFactor;
    }

    public void apply(double commission, double commissionFixed, double commissionVariable) {
        this.commission = commission;
        this.commissionFixed = commissionFixed;
        this.commissionVariable = commissionVariable;
    }

    public void add(double commission, double commissionFixed, double commissionVariable) {
        this.commission += commission;
        this.commissionFixed += commissionFixed;
        this.commissionVariable += commissionVariable;
    }

    public void adjustCommissionProperties(double commissionFactor, double fixedCommissionFactor, double variableCommissionFactor) {
        this.commission -= premiumPaid * commissionFactor;
        commissionFixed -= premiumPaid * fixedCommissionFactor;
        commissionVariable -= premiumPaid * variableCommissionFactor;
    }

    public boolean sameContent(CededUnderwritingInfoPacket other) {
        return super.sameContent(other)
                && premiumPaidFixed == other.getPremiumPaidFixed()
                && premiumPaidVariable == other.getPremiumPaidVariable()
                && commission == other.getCommission()
                && commissionFixed == other.getCommissionFixed()
                && commissionVariable == other.getCommissionVariable();
    }

    private static final String PREMIUM_WRITTEN = "premiumWritten";
    private static final String PREMIUM_PAID = "premiumPaid";
    private static final String PREMIUM_PAID_FIXED = "premiumPaidFixed";
    private static final String PREMIUM_PAID_VARIABLE = "premiumPaidVariable";
    private static final String COMMISSION = "commission";
    private static final String COMMISSION_FIXED = "commissionFixed";
    private static final String COMMISSION_VARIABLE = "commissionVariable";
    /**
     *
     * @return
     * @throws IllegalAccessException
     */
    @Override
    public Map<String, Number> getValuesToSave() throws IllegalAccessException {
        Map<String, Number> map = new HashMap<String, Number>();
        map.put(PREMIUM_WRITTEN, premiumWritten);
        map.put(PREMIUM_PAID, premiumPaid);
        map.put(PREMIUM_PAID_FIXED, premiumPaidFixed);
        map.put(PREMIUM_PAID_VARIABLE, premiumPaidVariable);
        map.put(COMMISSION, commission);
        map.put(COMMISSION_FIXED, commissionFixed);
        map.put(COMMISSION_VARIABLE, commissionVariable);
        return map;
    }

    public final static List<String> FIELD_NAMES = Arrays.asList(PREMIUM_WRITTEN, PREMIUM_PAID, PREMIUM_PAID_FIXED,
            PREMIUM_PAID_VARIABLE, COMMISSION, COMMISSION_FIXED, COMMISSION_VARIABLE);

    @Override
    public List<String> getFieldNames() {
        return FIELD_NAMES;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(premiumWritten);
        result.append(SEPARATOR);
        result.append(premiumPaid);
        result.append(SEPARATOR);
        result.append(premiumPaidFixed);
        result.append(SEPARATOR);
        result.append(premiumPaidVariable);
        result.append(SEPARATOR);
        result.append(commission);
        result.append(SEPARATOR);
        result.append(commissionFixed);
        result.append(SEPARATOR);
        result.append(commissionVariable);
        result.append(SEPARATOR);
        result.append(numberOfPolicies);
        return result.toString();
    }

    public double getPremiumPaidFixed() {
        return premiumPaidFixed;
    }

    public void setPremiumPaidFixed(double premiumPaidFixed) {
        this.premiumPaidFixed = premiumPaidFixed;
    }

    public double getPremiumPaidVariable() {
        return premiumPaidVariable;
    }

    public void setPremiumPaidVariable(double premiumPaidVariable) {
        this.premiumPaidVariable = premiumPaidVariable;
    }

    public double getCommission() {
        return commission;
    }

    public void setCommission(double commission) {
        this.commission = commission;
    }

    public double getCommissionFixed() {
        return commissionFixed;
    }

    public void setCommissionFixed(double commissionFixed) {
        this.commissionFixed = commissionFixed;
    }

    public double getCommissionVariable() {
        return commissionVariable;
    }

    public void setCommissionVariable(double commissionVariable) {
        this.commissionVariable = commissionVariable;
    }

    private static final String SEPARATOR = ", ";

    public UnderwritingInfoPacket getOriginal() {
        return original;
    }

    public void setOriginal(UnderwritingInfoPacket original) {
        this.original = original;
    }
}
