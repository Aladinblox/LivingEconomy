package com.yourname.livingeconomy;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

public class Loan {
    private final String loanId;
    private final double principalAmount;
    private final double interestRate; // Stored as decimal, e.g., 0.01 for 1%
    private final long issueDateMillis; // System.currentTimeMillis()
    private final long dueDateMillis;
    private double amountDue;       // Principal + Calculated Interest
    private boolean repaid;

    public Loan(double principalAmount, double interestRate, long issueDateMillis, long dueDateMillis, double amountDue) {
        this.loanId = UUID.randomUUID().toString();
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.issueDateMillis = issueDateMillis;
        this.dueDateMillis = dueDateMillis;
        this.amountDue = amountDue;
        this.repaid = false;
    }

    // Constructor for loading from config (Map)
    @SuppressWarnings("unchecked")
    public Loan(Map<String, Object> map) {
        this.loanId = (String) map.get("loanId");
        this.principalAmount = ((Number) map.get("principalAmount")).doubleValue();
        this.interestRate = ((Number) map.get("interestRate")).doubleValue();
        this.issueDateMillis = ((Number) map.get("issueDateMillis")).longValue();
        this.dueDateMillis = ((Number) map.get("dueDateMillis")).longValue();
        this.amountDue = ((Number) map.get("amountDue")).doubleValue();
        this.repaid = (Boolean) map.getOrDefault("repaid", false);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("loanId", loanId);
        map.put("principalAmount", principalAmount);
        map.put("interestRate", interestRate);
        map.put("issueDateMillis", issueDateMillis);
        map.put("dueDateMillis", dueDateMillis);
        map.put("amountDue", amountDue);
        map.put("repaid", repaid);
        return map;
    }

    // Getters
    public String getLoanId() { return loanId; }
    public double getPrincipalAmount() { return principalAmount; }
    public double getInterestRate() { return interestRate; }
    public long getIssueDateMillis() { return issueDateMillis; }
    public long getDueDateMillis() { return dueDateMillis; }
    public double getAmountDue() { return amountDue; }
    public boolean isRepaid() { return repaid; }

    public void setRepaid(boolean repaid) { this.repaid = repaid; }

    public boolean isOverdue() {
        return !repaid && System.currentTimeMillis() > dueDateMillis;
    }
}
