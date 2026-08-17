public final class AmountInWords {
    private static final String[] ONE={"","One","Two","Three","Four","Five","Six","Seven","Eight","Nine","Ten","Eleven","Twelve","Thirteen","Fourteen","Fifteen","Sixteen","Seventeen","Eighteen","Nineteen"};
    private static final String[] TEN={"","","Twenty","Thirty","Forty","Fifty","Sixty","Seventy","Eighty","Ninety"};
    public static String rupees(double amount){long n=Math.max(0,Math.round(amount));if(n==0)return "Zero Rupees Only";return part(n/10000000)+unit(n/100000,"Lakh")+unit(n/1000,"Thousand")+unit(n%1000,"")+"Rupees Only";}
    private static String unit(long n,String suffix){n%=suffix.equals("Lakh")?100:suffix.equals("Thousand")?100:1000;return n==0?"":part(n)+suffix+" ";}private static String part(long n){if(n<20)return ONE[(int)n]+" ";if(n<100)return TEN[(int)(n/10)]+" "+ONE[(int)(n%10)]+" ";return ONE[(int)(n/100)]+" Hundred "+part(n%100);}private AmountInWords(){}
}
