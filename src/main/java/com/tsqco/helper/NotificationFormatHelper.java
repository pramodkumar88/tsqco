package com.tsqco.helper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationFormatHelper {

    public static String dailyTransactionReport(List<String> rows) {
        StringBuilder message = new StringBuilder("*Transaction Report:*\n\n");
        message.append("| Token  | Symbol  | Action  | Quantity | Price   | Time            |\n");
        message.append("|--------|---------|---------|----------|---------|-----------------|\n");
        for (String row : rows) {
            String[] columns = row.split(",");
            message.append(String.format("| %-6s | %-7s | %-7s | %-8s | %-7s | %-15s |\n",
                    columns[0].trim(), columns[1].trim(), columns[2].trim(), columns[3].trim(), columns[4].trim(), columns[5].trim()));
        }
        message.append(String.format("\n_Total Transactions: %d_", rows.size()));
        return message.toString();
    }

    //EX: String row = "AAPL,NASDAQ,Buy,100,150.00,2025-01-01 12:30";
    // Method to format from a single String
    public static String stockTransactionUpdate(String data) {
        StringBuilder message = new StringBuilder("*Transaction Report:*\n\n");
        message.append("| Token  | Symbol  | Action  | Quantity | Price   | Time            |\n");
        message.append("|--------|---------|---------|----------|---------|-----------------|\n");
        String[] columns = data.split(",");
        message.append(String.format("| %-6s | %-7s | %-7s | %-8s | %-7s | %-15s |\n",
                columns[0].trim(), columns[1].trim(), columns[2].trim(), columns[3].trim(), columns[4].trim(), columns[5].trim()));

        return message.toString();
    }

    public static String systemFailureAlert(String reason, LocalDateTime timestamp, String action) {
        return String.format("\uD83D\uDD34 *CRITICAL: Application is Down* \uD83D\uDD34\n" +
                        "Reason: %s\n" +
                        "Timestamp: %s\n" +
                        "Action: %s\n",
                reason,
                timestamp != null ? timestamp.toString() : LocalDateTime.now().toString(),
                action != null ? action : "Please investigate the issue immediately.");
    }

    public static String jobCompletionAlert(String jobName, LocalDateTime timestamp, String details) {
        return String.format("\uD83D\uDFE2 *JOB COMPLETED: %s* \uD83D\uDFE2\n" +
                        "Timestamp: %s\n" +
                        "Details: %s\n",
                jobName,
                timestamp != null ? timestamp.toString() : LocalDateTime.now().toString(),
                details != null ? details : "Job completed successfully.");
    }

    public static String jobFailureAlert(String jobName, LocalDateTime timestamp, String errorDetails) {
        return String.format("\uD83D\uDD34 *JOB FAILED: %s* \uD83D\uDD34\n" +
                        "Timestamp: %s\n" +
                        "Error: %s\n" +
                        "Action: Please investigate and resolve the issue.",
                jobName,
                timestamp != null ? timestamp.toString() : LocalDateTime.now().toString(),
                errorDetails != null ? errorDetails : "Unknown error occurred.");
    }


    public static String genericNotification(String title, String content) {
        return String.format("*%s*\n\n%s", title, content);
    }

    public static String tokenSubscriptionFailureAlert(String token, String reason, LocalDateTime timestamp, String action) {
        return String.format("\u26A0 *ALERT: Token Subscription Failed* \u26A0\n" +
                        "Token: %s\n" +
                        "Reason: %s\n" +
                        "Timestamp: %s\n" +
                        "Action: %s\n",
                token != null ? token : "Unknown Token",
                reason != null ? reason : "No specific reason provided.",
                timestamp != null ? timestamp.toString() : LocalDateTime.now().toString(),
                action != null ? action : "Please retry or check the token details.");
    }

    public static String tokenUnsubscriptionFailureAlert(String token, String reason, LocalDateTime timestamp, String action) {
        return String.format("\u26A0 *ALERT: Token Unsubscription Failed* \u26A0\n" +
                        "Token: %s\n" +
                        "Reason: %s\n" +
                        "Timestamp: %s\n" +
                        "Action: %s\n",
                token != null ? token : "Unknown Token",
                reason != null ? reason : "No specific reason provided.",
                timestamp != null ? timestamp.toString() : LocalDateTime.now().toString(),
                action != null ? action : "Please retry or check the token details.");
    }

}
