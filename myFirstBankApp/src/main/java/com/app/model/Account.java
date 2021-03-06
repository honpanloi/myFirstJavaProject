package com.app.model;

public class Account {
	private long number;
	private String account_type;
	private String date_created;
	private long owner_id;
	private String status;
	private long approved_by;
	private long rejected_by;
	private double current_balance;
	
	public long getNumber() {
		return number;
	}
	public void setNumber(long number) {
		this.number = number;
	}
	public String getAccount_type() {
		return account_type;
	}
	public void setAccount_type(String account_type) {
		this.account_type = account_type;
	}
	public String getDate_created() {
		return date_created;
	}
	public void setDate_created(String date_created) {
		this.date_created = date_created;
	}
	public long getOwner_id() {
		return owner_id;
	}
	public void setOwner_id(long owner_id) {
		this.owner_id = owner_id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public long getApproved_by() {
		return approved_by;
	}
	public void setApproved_by(long approved_by) {
		this.approved_by = approved_by;
	}
	public long getRejected_by() {
		return rejected_by;
	}
	public void setRejected_by(long rejected_by) {
		this.rejected_by = rejected_by;
	}
	public double getCurrent_balance() {
		return current_balance;
	}
	public void setCurrent_balance(double current_balance) {
		this.current_balance = current_balance;
	}
	
	public Account() {
		
	}
	
	public Account(long number, String account_type, String date_created, long owner_id, String status, int approved_by,
			int rejected_by, double current_balance) {
		super();
		this.number = number;
		this.account_type = account_type;
		this.date_created = date_created;
		this.owner_id = owner_id;
		this.status = status;
		this.approved_by = approved_by;
		this.rejected_by = rejected_by;
		this.current_balance = current_balance;
	}
	
	@Override
	public String toString() {
		return "Account Number: " + number + "\n" + ", account_type=" + account_type + ", date_created=" + date_created
				+ ", owner_id=" + owner_id + ", status=" + status + ", approved_by=" + approved_by + ", rejected_by="
				+ rejected_by + ", current_balance=" + current_balance + "]";
	}
	
	public String getAccountBalance() {
		String result = null;
		result =	"Account Number: " + number +"\n"+
					"Account type: "+ account_type+"\n"+
					"Current Balance: $"+ current_balance;
		return result;
	}
	
	public String getPrintedAccountType() {
		String result = null;
		switch (account_type) {
		case "basic_checking":
			result = "Basic Checking";
			break;
		case "basic_saving":
			result = "Basic Saving";
			break;
		case "prem_checking":
			result = "Premium Checking";
			break;
		case "prem_saving":
			result = "Premium Saving";
			break;

		default:
			break;
		}
		return result;
	}
	
	public String getPrintedAccountStatus() {
		String result = null;
		switch (status) {
		case "active":
			result = "Active";
			break;
		case "closed":
			result = "Closed";
			break;
		case "frozen":
			result = "Frozen";
			break;
		case "not_yet_approved":
			result = "Waiting for Approval";
			break;
		case "rejected":
			result = "Rejected";
			break;

		default:
			break;
		}
		return result;
	}
	
	
	public boolean equals(Account a) {
		boolean b = true;
		
		if(!(this.account_type.toString().equals(a.account_type.toString()))) {
			System.out.println(1);
			return false;
		}
		if(!(this.approved_by==a.approved_by)) {
			System.out.println(1);
			return false;
		}
		if(!(this.current_balance==a.current_balance)) {
			System.out.println(1);
			return false;
		}
		if(!(this.date_created.toString().equals(a.date_created.toString()))) {
			System.out.println(1);
			return false;
		}
		if(!(this.number==a.number)) {
			System.out.println(1);
			return false;
		}
		if(!(this.owner_id==a.owner_id)) {
			System.out.println(1);
			return false;
		}
		if(!(this.rejected_by==a.rejected_by)) {
			System.out.println(1);
			return false;
		}
		if(!(this.status.toString().equals(a.status.toString()))) {
			System.out.println(1);
			return false;
		}
		return b;
	}
	
}
