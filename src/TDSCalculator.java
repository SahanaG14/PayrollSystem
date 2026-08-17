public final class TDSCalculator {
    private static final double STANDARD_DEDUCTION=75000.0,REBATTE_LIMIT=1200000.0,CESS_RATE=0.04;
    public static double annualTax(double annualGross){double taxableIncome=Math.max(0.0,annualGross-STANDARD_DEDUCTION);if(taxableIncome<=REBATTE_LIMIT)return 0.0;double baseAnnualTax=Math.min(calculateSlabTax(taxableIncome),taxableIncome-REBATTE_LIMIT),surchargeRate=0,threshold=0;for(SurchargeSlabStore.Slab slab:SurchargeSlabStore.list())if(taxableIncome>=slab.from()&&taxableIncome<=slab.to()){surchargeRate=slab.rate();threshold=slab.from()-1;break;}double totalBeforeCess=baseAnnualTax;if(surchargeRate>0){double rawSurcharge=baseAnnualTax*surchargeRate/100.0,taxAtThreshold=baseTaxAt(threshold),maxTaxAllowed=taxAtThreshold+(taxableIncome-threshold);totalBeforeCess=Math.min(baseAnnualTax+rawSurcharge,maxTaxAllowed);}return Math.round(totalBeforeCess*(1.0+CESS_RATE));}
    private static double baseTaxAt(double taxableIncome){return taxableIncome<=REBATTE_LIMIT?0:Math.min(calculateSlabTax(taxableIncome),taxableIncome-REBATTE_LIMIT);}
    private static double calculateSlabTax(double taxableIncome){double tax=0.0;for(TaxSlabStore.Slab slab:TaxSlabStore.list())tax+=Math.max(0.0,Math.min(taxableIncome,slab.to())-slab.from())*slab.rate()/100.0;return tax;}
    public static double monthlyTds(double annualGross){return Money.round(annualTax(annualGross)/12.0);}
    public static double monthlyTds(CTCStore.Value ctcRecord){return ctcRecord==null?0.0:monthlyTds(ctcRecord.totalAnnualGrossSalary());}
    public static double calculateMonthlyTDS(String employeeId){return monthlyTds(CTCStore.get(employeeId));}
    private TDSCalculator(){}
}
