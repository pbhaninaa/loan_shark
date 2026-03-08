package com.loanshark.mobile;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.loanshark.mobile.data.NotificationItem;
import com.loanshark.mobile.data.ScheduleResponse;
import com.loanshark.mobile.data.VerificationResponse;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private LoanViewModel viewModel;
    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private TextInputEditText editLoanAmount;
    private TextInputEditText editLoanId;
    private TextView textMessage;
    private TextView textBorrowerStatus;
    private TextView textVerification;
    private TextView textLoanDetail;
    private TextView textSchedule;
    private TextView textNotifications;
    private LinearLayout panelLogin;
    private LinearLayout panelRegister;
    private LinearLayout panelVerification;
    private LinearLayout panelLoan;
    private LinearLayout panelSchedule;
    private LinearLayout panelNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getApplication())).get(LoanViewModel.class);

        textMessage = findViewById(R.id.textMessage);
        textBorrowerStatus = findViewById(R.id.textBorrowerStatus);
        textVerification = findViewById(R.id.textVerification);
        textLoanDetail = findViewById(R.id.textLoanDetail);
        textSchedule = findViewById(R.id.textSchedule);
        textNotifications = findViewById(R.id.textNotifications);

        panelLogin = findViewById(R.id.panelLogin);
        panelRegister = findViewById(R.id.panelRegister);
        panelVerification = findViewById(R.id.panelVerification);
        panelLoan = findViewById(R.id.panelLoan);
        panelSchedule = findViewById(R.id.panelSchedule);
        panelNotifications = findViewById(R.id.panelNotifications);

        editUsername = findViewById(R.id.editUsername);
        editPassword = findViewById(R.id.editPassword);
        editLoanAmount = findViewById(R.id.editLoanAmount);
        editLoanId = findViewById(R.id.editLoanId);

        viewModel.getMessage().observe(this, msg -> {
            if (msg != null) textMessage.setText(msg);
        });
        viewModel.getBorrowerStatus().observe(this, status -> {
            if (status != null) {
                textBorrowerStatus.setVisibility(View.VISIBLE);
                textBorrowerStatus.setText("Borrower status: " + status);
            }
        });
        viewModel.getVerification().observe(this, this::showVerification);
        viewModel.getCurrentLoan().observe(this, loan -> {
            if (loan != null) {
                textLoanDetail.setVisibility(View.VISIBLE);
                textLoanDetail.setText(String.format(Locale.US, "Loan #%s | %s | Total: R %.2f", loan.id, loan.status, loan.totalAmount));
            }
        });
        viewModel.getSchedule().observe(this, this::showSchedule);
        viewModel.getNotifications().observe(this, this::showNotifications);

        findViewById(R.id.btnLogin).setOnClickListener(v -> showPanel(panelLogin));
        findViewById(R.id.btnRegister).setOnClickListener(v -> showPanel(panelRegister));
        findViewById(R.id.btnVerification).setOnClickListener(v -> {
            viewModel.loadBorrowerVerification();
            showPanel(panelVerification);
        });
        findViewById(R.id.btnLoan).setOnClickListener(v -> showPanel(panelLoan));
        findViewById(R.id.btnSchedule).setOnClickListener(v -> showPanel(panelSchedule));
        findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            viewModel.loadNotifications();
            showPanel(panelNotifications);
        });

        findViewById(R.id.btnDoLogin).setOnClickListener(v -> {
            String u = editUsername.getText() != null ? editUsername.getText().toString() : "";
            String p = editPassword.getText() != null ? editPassword.getText().toString() : "";
            viewModel.login(u, p);
        });

        findViewById(R.id.btnRefreshVerification).setOnClickListener(v -> viewModel.loadBorrowerVerification());

        findViewById(R.id.btnApplyLoan).setOnClickListener(v -> {
            try {
                double amount = Double.parseDouble(editLoanAmount.getText() != null ? editLoanAmount.getText().toString() : "0");
                viewModel.applyForLoan(amount);
            } catch (NumberFormatException e) {
                viewModel.publishMessage("Enter a valid amount");
            }
        });

        findViewById(R.id.btnLoadLoan).setOnClickListener(v -> {
            String id = editLoanId.getText() != null ? editLoanId.getText().toString().trim() : "";
            if (!id.isEmpty()) viewModel.loadLoanDetails(id);
            else viewModel.publishMessage("Enter a loan ID");
        });
    }

    private void showPanel(LinearLayout panel) {
        panelLogin.setVisibility(panel == panelLogin ? View.VISIBLE : View.GONE);
        panelRegister.setVisibility(panel == panelRegister ? View.VISIBLE : View.GONE);
        panelVerification.setVisibility(panel == panelVerification ? View.VISIBLE : View.GONE);
        panelLoan.setVisibility(panel == panelLoan ? View.VISIBLE : View.GONE);
        panelSchedule.setVisibility(panel == panelSchedule ? View.VISIBLE : View.GONE);
        panelNotifications.setVisibility(panel == panelNotifications ? View.VISIBLE : View.GONE);
    }

    private void showVerification(VerificationResponse v) {
        if (v == null) {
            textVerification.setText("No verification record loaded yet");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Status: ").append(v.status).append("\n");
        sb.append("Location: ").append(v.locationName != null ? v.locationName : "-").append("\n");
        sb.append("SA ID valid: ").append(v.saIdValid).append("\n");
        sb.append("Details matched: ").append(v.detailsMatched).append("\n");
        sb.append("Face matched: ").append(v.faceMatched).append("\n");
        sb.append("Review notes: ").append(v.reviewNotes != null ? v.reviewNotes : "Awaiting review");
        textVerification.setText(sb.toString());
    }

    private void showSchedule(List<ScheduleResponse> list) {
        if (list == null || list.isEmpty()) {
            textSchedule.setText("Load a loan first to see schedule.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (ScheduleResponse s : list) {
            sb.append(String.format(Locale.US, "Installment %d | Due: %s | R %.2f | %s\n", s.installmentNumber, s.dueDate, s.amountDue, s.status));
        }
        textSchedule.setText(sb.toString());
    }

    private void showNotifications(List<NotificationItem> list) {
        if (list == null || list.isEmpty()) {
            textNotifications.setText("No notifications.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (NotificationItem n : list) {
            sb.append(n.channel).append(": ").append(n.message).append("\n");
        }
        textNotifications.setText(sb.toString());
    }
}
