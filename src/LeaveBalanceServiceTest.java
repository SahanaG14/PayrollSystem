/** Lightweight no-dependency regression checks for the carry-forward cap arithmetic. */
public final class LeaveBalanceServiceTest {
    public static void main(String[] args) {
        equal(4, LeaveBalanceService.carried(0, 4, 0, 6), "below maximum");
        equal(6, LeaveBalanceService.carried(4, 5, 0, 6), "cap at maximum");
        equal(0, LeaveBalanceService.carried(0, 4, 0, 0), "zero maximum");
        equal(7.5, LeaveBalanceService.carried(0, 7.5, 0, 10), "half day");
        equal(2, LeaveBalanceService.carried(4, 2, 4, 6), "usage deducted before carry");
        equal(0, LeaveBalanceService.carried(0, 2, 5, 6), "never negative");
    }
    private static void equal(double expected,double actual,String label){if(Math.abs(expected-actual)>.001)throw new AssertionError(label+": expected "+expected+", got "+actual);}
}
