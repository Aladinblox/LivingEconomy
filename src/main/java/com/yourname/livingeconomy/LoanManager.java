package com.yourname.livingeconomy;

import org.bukkit.ChatColor;
import org.bukkit.conversations.*;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class LoanManager {
    private final LivingEconomy plugin;
    private final ConfigManager configManager;
    private final DataManager dataManager;

    public LoanManager(LivingEconomy plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.dataManager = plugin.getDataManager();
    }

    public void requestLoanConversation(Player player, PlayerData playerData) {
        if (playerData.getActiveLoans().size() >= configManager.getMaxActiveLoans()) {
            player.sendMessage(ChatColor.RED + "You have reached the maximum number of active loans (" + configManager.getMaxActiveLoans() + ").");
            return;
        }

        player.closeInventory();
        ConversationFactory factory = new ConversationFactory(plugin);
        Conversation conv = factory.withFirstPrompt(new NumericPrompt() {
            @Override
            public String getPromptText(ConversationContext context) {
                return ChatColor.GOLD + "Enter loan amount (max " + String.format("%.2f",configManager.getMaxLoanAmount()) + ", or type 'cancel'):";
            }

            @Override
            protected Prompt acceptValidatedInput(ConversationContext context, Number input) {
                double amount = input.doubleValue();
                if (amount <= 0 || amount > configManager.getMaxLoanAmount()) {
                    context.getForWhom().sendRawMessage(ChatColor.RED + "Invalid amount. Must be between 0.01 and " + String.format("%.2f",configManager.getMaxLoanAmount()) + ".");
                    return this; // Re-prompt
                }

                double interestRate = configManager.getLoanInterestRateDaily();
                int repaymentDays = configManager.getLoanRepaymentPeriodDays();
                long issueDate = System.currentTimeMillis();
                long dueDate = issueDate + TimeUnit.DAYS.toMillis(repaymentDays);
                // Simple interest: P * R * T. Here T is the period.
                double totalInterest = amount * interestRate * repaymentDays;
                double amountDue = amount + totalInterest;

                Loan newLoan = new Loan(amount, interestRate, issueDate, dueDate, amountDue);
                playerData.addLoan(newLoan);
                // TODO: Ensure "gold" or the primary currency for loans exists or is configurable from config.yml (e.g. economy.loan.currency)
                String loanCurrency = plugin.getConfigManager().getMainConfig().getString("economy.loan.currency", "gold");
                playerData.giveToWallet(loanCurrency, amount);

                playerData.save();
                context.getForWhom().sendRawMessage(ChatColor.GREEN + "Loan of " + String.format("%.2f",amount) + " " + loanCurrency + " approved!");
                context.getForWhom().sendRawMessage(ChatColor.YELLOW + "Amount due: " + String.format("%.2f",amountDue) + " " + loanCurrency + " by " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date(dueDate)));
                return Prompt.END_OF_CONVERSATION;
            }

            @Override
            protected String getFailedValidationText(ConversationContext context, String invalidInput) {
                if (invalidInput.equalsIgnoreCase("cancel")) return null;
                return ChatColor.RED + "Invalid number. Please enter a numeric amount or 'cancel'.";
            }

            @Override
            protected boolean isInputValid(ConversationContext context, String input) {
                if (input.equalsIgnoreCase("cancel")) return true;
                try {
                    double val = Double.parseDouble(input);
                    return val > 0 && val <= configManager.getMaxLoanAmount();
                } catch (NumberFormatException e) { return false; }
            }
        }).withLocalEcho(false).withEscapeSequence("cancel").withTimeout(30)
          .thatExcludesNonPlayersWithMessage("Console cannot manage loans.")
          .addConversationAbandonedListener(event -> {
              if (!event.gracefulExit()) player.sendMessage(ChatColor.RED + "Loan application timed out or cancelled.");
              // Optionally re-open LoanOfficeGUI: new LoanOfficeGUI(plugin, player).open();
          }).buildConversation(player);
        conv.begin();
    }

    public void repayLoanConversation(Player player, PlayerData playerData, String loanId) {
        Loan loan = playerData.getLoanById(loanId);
        if (loan == null || loan.isRepaid()) {
            player.sendMessage(ChatColor.RED + "Loan not found or already repaid.");
            return;
        }

        player.closeInventory();
        ConversationFactory factory = new ConversationFactory(plugin);
        Conversation conv = factory.withFirstPrompt(new StringPrompt() {
            @Override
            public String getPromptText(ConversationContext context) {
                // TODO: Use actual loan currency
                String loanCurrency = plugin.getConfigManager().getMainConfig().getString("economy.loan.currency", "gold");
                return ChatColor.GOLD + "Repay loan of " + String.format("%.2f", loan.getAmountDue()) + " " + loanCurrency + "? Type 'confirm' or 'cancel':";
            }

            @Override
            public Prompt acceptInput(ConversationContext context, String input) {
                if (input.equalsIgnoreCase("confirm")) {
                    String loanCurrency = plugin.getConfigManager().getMainConfig().getString("economy.loan.currency", "gold");
                    if (playerData.withdrawFromBank(loanCurrency, loan.getAmountDue())) {
                        loan.setRepaid(true); // Mark as repaid
                        // For now, just marking as repaid. PlayerData.getActiveLoans() will still contain it.
                        // A filter can be applied in the GUI, or a periodic cleanup task can remove fully repaid loans.
                        // Or, call playerData.removeLoan(loanId); if it should be immediately removed from the list.
                        playerData.save();
                        context.getForWhom().sendRawMessage(ChatColor.GREEN + "Loan repaid successfully!");
                    } else {
                        context.getForWhom().sendRawMessage(ChatColor.RED + "Insufficient " + loanCurrency + " in bank to repay the loan.");
                    }
                } else {
                    context.getForWhom().sendRawMessage(ChatColor.YELLOW + "Loan repayment cancelled.");
                }
                return Prompt.END_OF_CONVERSATION;
            }
        }).withLocalEcho(false).withEscapeSequence("cancel").withTimeout(30)
          .addConversationAbandonedListener(event -> {
              if (!event.gracefulExit()) player.sendMessage(ChatColor.RED + "Loan repayment timed out or cancelled.");
               // new LoanOfficeGUI(plugin, player).open();
          }).buildConversation(player);
        conv.begin();
    }
}
