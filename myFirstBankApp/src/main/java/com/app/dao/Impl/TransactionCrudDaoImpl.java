package com.app.dao.Impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.app.dao.AccountCrudDAO;
import com.app.dao.TransactionCrudDao;
import com.app.dao.dbutil.PostgresqlConnection;
import com.app.exception.BusinessException;
import com.app.main.Main;
import com.app.model.Account;
import com.app.model.Transaction;
import com.app.util.Tool;

public class TransactionCrudDaoImpl implements TransactionCrudDao {

	private static Logger log = Logger.getLogger(Main.class);
	
	@Override
	public int createDepositeOnlyTransaction(long accountNumber, double depositAmount) throws BusinessException {
		int c = 0;
		long transactionNum = 0;
		try(Connection connection = PostgresqlConnection.getConnection()){
			
			//generate transaction
			String sql = "insert into my_bank_app.\"transaction\"(\"type\", status, time_requested, initiator_acc, amount) values (?,?,?,?,?)";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, "atm_deposite");
			preparedStatement.setString(2, "pending");
			//create and store the currentDate as a reference to find the created transaction
			String currentDate = Tool.getPrintedCurrentDate();
			preparedStatement.setString(3, currentDate);
			
			preparedStatement.setDouble(4, accountNumber);
			preparedStatement.setDouble(5, depositAmount);
			
			c += preparedStatement.executeUpdate();
			
			Tool.get2SecondProcessingTime();
			
			connection.setAutoCommit(false);
			Savepoint sp1 = connection.setSavepoint();
			
			
			//acquire current balance
			String sql2 = "select current_balance from my_bank_app.account where \"number\" = ?";
			PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
			preparedStatement2.setLong(1, accountNumber);
			ResultSet resultSet = preparedStatement2.executeQuery();
			double currentBalance = 0;
			if(resultSet.next()) {
				currentBalance = resultSet.getDouble("current_balance");
				c+=1;
			}
			
			//get projected balance
			double projectedBalance = currentBalance + depositAmount;
			
			//deposit the money into the account
			String sql3 = "update my_bank_app.account set current_balance = ? where \"number\" = ?";
			PreparedStatement preparedStatement3 = connection.prepareStatement(sql3);
			preparedStatement3.setDouble(1, projectedBalance);
			preparedStatement3.setLong(2, accountNumber);
			
			c += preparedStatement3.executeUpdate();
			
			//Search for the right transaction by account number and date created
			String sql4 = "select \"trans_number\" from my_bank_app.\"transaction\" where time_requested = ? and initiator_acc = ?";
			PreparedStatement preparedStatement4 = connection.prepareStatement(sql4);
			preparedStatement4.setString(1, currentDate);
			preparedStatement4.setLong(2, accountNumber);
			ResultSet resultSet1 = preparedStatement4.executeQuery();
			
			if(resultSet1.next()) {
				transactionNum = resultSet1.getLong("trans_number");
				c+=1;
			}
			
			//update the transaction status to complete
			//update the transaction completing time
			//mark which account it deposited into
			//mark which account it deposited into
			String sql5 = "update my_bank_app.\"transaction\" set \"status\" = 'completed', time_completed = ?, deposit_to = ?, balance_after_deposit = ? where trans_number = ?";
			PreparedStatement preparedStatement5 = connection.prepareStatement(sql5);
			preparedStatement5.setString(1, Tool.getPrintedCurrentDate());
			preparedStatement5.setLong(2, accountNumber);
			preparedStatement5.setDouble(3, projectedBalance);
			preparedStatement5.setLong(4, transactionNum);
			
			
			c += preparedStatement5.executeUpdate();
			
			
			if(c==5) {
				connection.commit();
				connection.setAutoCommit(true);
				log.info("Deposit completed.");
				Main.spaceOutTheOldMessages();
			}else {
				log.info("An error has occurred. Transaction incompleted");
				connection.rollback(sp1);
				Main.spaceOutTheOldMessages();
			}
			
			
			
		} catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
			putCanceledToTheTransactionStatus(transactionNum); 
			
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
			putCanceledToTheTransactionStatus(transactionNum); 
		}finally {
			if(c<5) {
				putCanceledToTheTransactionStatus(transactionNum);
			}
		}

		return c;
	}
	
	private void putCanceledToTheTransactionStatus(long transactionNum) {
		try(Connection connection = PostgresqlConnection.getConnection()){
			//if any of the the previous step failed, update the transaction status to 'canceled'
			String sql9 = "update my_bank_app.\"transaction\" set \"status\" = 'canceled' where \"trans_number\" = ?";
			PreparedStatement preparedStatement9 = connection.prepareStatement(sql9);
			preparedStatement9.setLong(1, transactionNum);
			preparedStatement9.executeUpdate();
		} catch (ClassNotFoundException | SQLException e1) {
			log.info("Unable to put 'cancel' as status to the transaction. Please contact admin.");
		}
	}

	@Override
	public int createWithdrawOnlyTransaction(long accountNumber, double withdrawAmount) throws BusinessException {
		int c = 0;
		long transactionNum = 0;
		try(Connection connection = PostgresqlConnection.getConnection()){
			
			//generate transaction
			String sql = "insert into my_bank_app.\"transaction\"(\"type\", status, time_requested, initiator_acc, amount) values (?,?,?,?,?)";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, "atm_withdraw");
			preparedStatement.setString(2, "pending");
			//create and store the currentDate as a reference to find the created transaction
			String currentDate = Tool.getPrintedCurrentDate();
			preparedStatement.setString(3, currentDate);
			
			preparedStatement.setDouble(4, accountNumber);
			preparedStatement.setDouble(5, withdrawAmount);
			
			c += preparedStatement.executeUpdate();
			
			Tool.get2SecondProcessingTime();
			
			connection.setAutoCommit(false);
			Savepoint sp1 = connection.setSavepoint();
			
			
			//acquire current balance
			String sql2 = "select current_balance, account_type from my_bank_app.account where \"number\" = ?";
			PreparedStatement preparedStatement2 = connection.prepareStatement(sql2);
			preparedStatement2.setLong(1, accountNumber);
			ResultSet resultSet = preparedStatement2.executeQuery();
			double currentBalance = 0;
			String accountType = null;
			if(resultSet.next()) {
				currentBalance = resultSet.getDouble("current_balance");
				accountType = resultSet.getString("account_type");
				c+=1;
			}
			
			//acquire the maximum overdrawn amount
			double maximumOverdrawnAmount = 0;
			String sql6 = "select overdrawn_amount from my_bank_app.account_type where \"type\" = ?";
			PreparedStatement preparedStatement6 = connection.prepareStatement(sql6);
			preparedStatement6.setString(1, accountType);
			ResultSet resultSet3 = preparedStatement6.executeQuery();
			if(resultSet3.next()) {
				maximumOverdrawnAmount = resultSet3.getDouble("overdrawn_amount");
				c+=1;
			}
			
			//get projected balance
			double projectedBalance = currentBalance - withdrawAmount;
			
			//if the projected balance would be lower than the maximum overdrawn amount of that account, transaction will be stopped
			maximumOverdrawnAmount = maximumOverdrawnAmount*-1;
			if(projectedBalance<maximumOverdrawnAmount) {
				throw new BusinessException("Your projected account balance will be lower than the maximum overdrawn amount: $"+maximumOverdrawnAmount*-1+"\n Withdraw unsuccessful. Please try again or visit a counter.");
			}
			
			//withdraw the money from the account
			String sql3 = "update my_bank_app.account set current_balance = ? where \"number\" = ?";
			PreparedStatement preparedStatement3 = connection.prepareStatement(sql3);
			preparedStatement3.setDouble(1, projectedBalance);
			preparedStatement3.setLong(2, accountNumber);
			
			c += preparedStatement3.executeUpdate();
			
			//Search for the right transaction by account number and date created
			String sql4 = "select \"trans_number\" from my_bank_app.\"transaction\" where time_requested = ? and initiator_acc = ?";
			PreparedStatement preparedStatement4 = connection.prepareStatement(sql4);
			preparedStatement4.setString(1, currentDate);
			preparedStatement4.setLong(2, accountNumber);
			ResultSet resultSet1 = preparedStatement4.executeQuery();
			
			sql = "select \"trans_number\" from my_bank_app.\"transaction\" where time_requested = ? and initiator_acc = ?";

			
			if(resultSet1.next()) {
				transactionNum = resultSet1.getLong("trans_number");
				c+=1;
			}
			
			//update the transaction status to complete
			//update the transaction completing time
			//mark which account it deposited into
			//mark which account it deposited into
			String sql5 = "update my_bank_app.\"transaction\" set \"status\" = 'completed', time_completed = ?, withdraw_from = ?, balance_after_withdraw = ? where trans_number = ?";
			PreparedStatement preparedStatement5 = connection.prepareStatement(sql5);
			preparedStatement5.setString(1, Tool.getPrintedCurrentDate());
			preparedStatement5.setLong(2, accountNumber);
			preparedStatement5.setDouble(3, projectedBalance);
			preparedStatement5.setLong(4, transactionNum);
			
			
			c += preparedStatement5.executeUpdate();
			
			
			if(c==6) {
				connection.commit();
				connection.setAutoCommit(true);
				log.info("Withdrawal completed.");
				Main.spaceOutTheOldMessages();
			}else {
				log.info("An error has occurred. Transaction incompleted");
				connection.rollback(sp1);
				Main.spaceOutTheOldMessages();
			}
			
			
			
		} catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
			
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
			 
		}finally {
			if(c<6) {
				putCanceledToTheTransactionStatus(transactionNum);
			}
		}

		return c;
	}

	@Override
	public int createTransferTransactionWhenBothAccountsBelongToTheSamePerson(
			long targetAccountNumberTransferTo, 
			long targetAccountNumberTransferFrom,
			double amount) throws BusinessException {
		int c = 0;
		long transactionNum = 0;
		AccountCrudDAO accountCrudDAO = new AccountCrudDAOImpl();
		try(Connection connection = PostgresqlConnection.getConnection()){
			
			//1.check if the TransferFrom has enough balance
			boolean isBalanceSufficient = false;
			Account accountFrom = accountCrudDAO.getAccountByAccountNum(targetAccountNumberTransferFrom);
			isBalanceSufficient = (accountFrom.getCurrent_balance()>=amount);
			if(!isBalanceSufficient) {
				log.info("The target account does not have enough fund. Please try again later.");
				return c;
			}
			c++;
			
			//2.create the transaction as pending
			String sql = "insert into my_bank_app.\"transaction\"(\"type\",status, time_requested, initiator_acc, amount,withdraw_from,deposit_to) values (?,?,?,?,?,?,?)";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, "transfer");
			preparedStatement.setString(2, "pending");
			String currentDate = Tool.getPrintedCurrentDate();
			preparedStatement.setString(3, currentDate);
			preparedStatement.setLong(4, accountFrom.getNumber());
			preparedStatement.setDouble(5, amount);
			preparedStatement.setLong(6, targetAccountNumberTransferFrom);
			preparedStatement.setLong(7, targetAccountNumberTransferTo);
			
			c += preparedStatement.executeUpdate();
			
			//2.1 add processing time
			Tool.get2SecondProcessingTime();
			//3.create save point
			connection.setAutoCommit(false);
			Savepoint sp1 = connection.setSavepoint();
			
			//4.subtract money from targetAccountNumberTransferFrom
			double projectedBalanceAfterWithdraw = accountFrom.getCurrent_balance()-amount;
			sql = "update my_bank_app.account set current_balance = ? where \"number\" = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setDouble(1, projectedBalanceAfterWithdraw);
			preparedStatement.setLong(2, targetAccountNumberTransferFrom);
			c += preparedStatement.executeUpdate();
			
			//5.add money to targetAccountNumberTransferTo
			Account accountTo = accountCrudDAO.getAccountByAccountNum(targetAccountNumberTransferTo);
			double projectedBalanceAfterDeposit = accountTo.getCurrent_balance()+amount;
			sql = "update my_bank_app.account set current_balance = ? where \"number\" = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setDouble(1, projectedBalanceAfterDeposit);
			preparedStatement.setLong(2, targetAccountNumberTransferTo);
			c += preparedStatement.executeUpdate();
			
			//6.search for the transaction created earlier
			sql = "select \"trans_number\" from my_bank_app.\"transaction\" where time_requested = ? and initiator_acc = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, currentDate);
			preparedStatement.setLong(2, targetAccountNumberTransferFrom);
			
			ResultSet resultSet = preparedStatement.executeQuery();
			
			if(resultSet.next()) {
				transactionNum = resultSet.getLong("trans_number");
				c+=1;
			}
			
			
			//7.update the transaction created earlier
			sql = "update my_bank_app.\"transaction\" set \"status\" = 'completed', time_completed = ?, balance_after_deposit = ?, balance_after_withdraw = ?  where trans_number = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, Tool.getPrintedCurrentDate());
			preparedStatement.setDouble(2, projectedBalanceAfterDeposit);
			preparedStatement.setDouble(3, projectedBalanceAfterWithdraw);
			preparedStatement.setLong(4, transactionNum);
			
			
			c += preparedStatement.executeUpdate();
			
			if(c==6) {
				connection.commit();
				connection.setAutoCommit(true);
				log.info("Transfer successful.");
				Main.spaceOutTheOldMessages();
			}else {
				connection.rollback(sp1);
				log.info("Transfer is not complete.");
			}
			
		} catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
			
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
			 
		}finally {
			if(c<6) {
				putCanceledToTheTransactionStatus(transactionNum);
			}
		}
		
		return c;
	}

	@Override
	public int createTransferTransactionToAnotherPerson(long targetAccountNumberTransferTo,
			long targetAccountNumberTransferFrom, double amount) throws BusinessException {
		int c = 0;
		long transactionNum = 0;
		AccountCrudDAO accountCrudDAO = new AccountCrudDAOImpl();
		try(Connection connection = PostgresqlConnection.getConnection()){
			
			//1.check if the TransferFrom has enough balance
			boolean isBalanceSufficient = false;
			Account accountFrom = accountCrudDAO.getAccountByAccountNum(targetAccountNumberTransferFrom);
			isBalanceSufficient = (accountFrom.getCurrent_balance()>=amount);
			if(!isBalanceSufficient) {
				log.info("The target account does not have enough fund. Please try again later.");
				return c;
			}
			c++;
			
			
			
			//2.create the transaction as pending
			String sql = "insert into my_bank_app.\"transaction\"(\"type\",status, time_requested, initiator_acc, amount,withdraw_from,deposit_to) values (?,?,?,?,?,?,?)";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, "transfer");
			preparedStatement.setString(2, "pending");
			String currentDate = Tool.getPrintedCurrentDate();
			preparedStatement.setString(3, currentDate);
			preparedStatement.setLong(4, accountFrom.getNumber());
			preparedStatement.setDouble(5, amount);
			preparedStatement.setLong(6, targetAccountNumberTransferFrom);
			preparedStatement.setLong(7, targetAccountNumberTransferTo);
			
			c += preparedStatement.executeUpdate();
			
			//2.1 add processing time
			Tool.get2SecondProcessingTime();
			//3.create save point
			connection.setAutoCommit(false);
			Savepoint sp1 = connection.setSavepoint();
			
			//4.subtract money from targetAccountNumberTransferFrom
			double projectedBalanceAfterWithdraw = accountFrom.getCurrent_balance()-amount;
			sql = "update my_bank_app.account set current_balance = ? where \"number\" = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setDouble(1, projectedBalanceAfterWithdraw);
			preparedStatement.setLong(2, targetAccountNumberTransferFrom);
			c += preparedStatement.executeUpdate();
			
			//5.search for the transaction created earlier
			sql = "select \"trans_number\" from my_bank_app.\"transaction\" where time_requested = ? and initiator_acc = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, currentDate);
			preparedStatement.setLong(2, targetAccountNumberTransferFrom);
			
			ResultSet resultSet = preparedStatement.executeQuery();
			
			if(resultSet.next()) {
				transactionNum = resultSet.getLong("trans_number");
				c+=1;
			}
			
			//6.update the transaction for how much it withdraw
			sql = "update my_bank_app.\"transaction\" set balance_after_withdraw = ? where trans_number = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setDouble(1, projectedBalanceAfterWithdraw);
			preparedStatement.setLong(2, transactionNum);
			c += preparedStatement.executeUpdate();
			
			
			if(c==5) {
				connection.commit();
				connection.setAutoCommit(true);
				log.info("Transfer requested.");
				log.info("If the other account holder does not accpet the transfer, or the account holder does not accept it "+
				"\nwithin 90 day, the transfer amount will be deposited back to your account.");
				Main.spaceOutTheOldMessages();
			}else {
				connection.rollback(sp1);
				log.info("Transfer is not complete.");
			}
			
		} catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
			
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
			 
		}finally {
			if(c<5) {
				putCanceledToTheTransactionStatus(transactionNum);
			}
		}
		
		return c;
	}

	@Override
	public List<Transaction> searchForIncomingTransactions(long depositToAccountNum) throws BusinessException {
		List<Transaction> result = null;
		
		try(Connection connection = PostgresqlConnection.getConnection()){
			
			String sql = "select * from my_bank_app.\"transaction\" where deposit_to = ? and \"status\" = 'pending'";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setLong(1,depositToAccountNum);
				
			ResultSet resultSet = preparedStatement.executeQuery();
				
			result = new ArrayList<Transaction>();
			while(resultSet.next()) {
				Transaction transcation = new Transaction();
				transcation.setTrans_number(resultSet.getLong("trans_number"));
				transcation.setType(resultSet.getString("type"));
				transcation.setWithdraw_from(resultSet.getLong("withdraw_from"));
				transcation.setDeposit_to(resultSet.getLong("deposit_to"));
				transcation.setStatus(resultSet.getString("status"));
				transcation.setTime_requested(resultSet.getString("time_requested"));
				transcation.setTime_completed(resultSet.getString("time_completed"));
				transcation.setAmount(resultSet.getDouble("amount"));
				transcation.setBalance_after_withdraw(resultSet.getDouble("balance_after_withdraw"));
				transcation.setBalance_after_deposit(resultSet.getDouble("balance_after_deposit"));
				transcation.setInitiator_acc(resultSet.getLong("initiator_acc"));
				result.add(transcation);
			}
			
		}catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
			
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
			 
		}
		return result;
	}

	@Override
	public int acceptAnIncomingTransfer(long trasactionNum) throws BusinessException {
		int c= 0;
		
		
		try(Connection connection = PostgresqlConnection.getConnection()){
			//gather all the transaction information
			Transaction transcation = null;
			transcation = getTranscationById(trasactionNum);
			c++;
			
			//Gather the current balance of the deposit to account
			String sql = "select current_balance, account_type from my_bank_app.account where \"number\" = ?";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setLong(1, transcation.getDeposit_to());
			ResultSet resultSet = preparedStatement.executeQuery();
			double currentBalance = 0;
			if(resultSet.next()) {
			currentBalance = resultSet.getDouble("current_balance");
			c+=1;
			}
			System.out.println("2");
			//calculate the projected balance
			double projectedbalance = currentBalance + transcation.getAmount();
		
			connection.setAutoCommit(false);
			Savepoint sp1 = connection.setSavepoint();
		
			//deposit the amount
			sql = "update my_bank_app.account set current_balance = ? where \"number\" = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setDouble(1, projectedbalance);
			preparedStatement.setLong(2, transcation.getDeposit_to());

			c += preparedStatement.executeUpdate();
			System.out.println("3");
			//update the status and date completed
			sql = "update my_bank_app.\"transaction\" set \"status\" = 'completed', time_completed = ?, balance_after_deposit = ? where trans_number = ?";
			preparedStatement = connection.prepareStatement(sql);
			preparedStatement.setString(1, Tool.getPrintedCurrentDate());
			preparedStatement.setDouble(2, projectedbalance);
			preparedStatement.setLong(3, transcation.getTrans_number());
			c += preparedStatement.executeUpdate();
			System.out.println("4");
			
			if(c==4) {
			connection.commit();
			connection.setAutoCommit(true);
			log.info("Transaction completed.");
			Main.spaceOutTheOldMessages();
			
			}else {
			connection.rollback(sp1);
			log.info("An error has occurred. Transaction Incomplete.");
			Main.spaceOutTheOldMessages();
		}
		
		
		} catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
		}
		return c;
	}
	
	public Transaction getTranscationById(long trasactionNum)throws BusinessException {
		Transaction transcation = null;
		try(Connection connection = PostgresqlConnection.getConnection()){
			
		String sql = "select * from my_bank_app.\"transaction\" where \"trans_number\" = ?";
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		preparedStatement.setLong(1, trasactionNum);
		ResultSet resultSet = preparedStatement.executeQuery();
		
		if(resultSet.next()) {
			transcation = new Transaction();
			transcation.setTrans_number(trasactionNum);
			transcation.setType(resultSet.getString("type"));
			transcation.setWithdraw_from(resultSet.getLong("withdraw_from"));
			transcation.setDeposit_to(resultSet.getLong("deposit_to"));
			transcation.setStatus(resultSet.getString("status"));
			transcation.setTime_requested(resultSet.getString("time_requested"));
			transcation.setTime_completed(resultSet.getString("time_completed"));
			transcation.setAmount(resultSet.getDouble("amount"));
			transcation.setBalance_after_withdraw(resultSet.getDouble("balance_after_withdraw"));
			transcation.setBalance_after_deposit(resultSet.getDouble("balance_after_deposit"));
			transcation.setInitiator_acc(resultSet.getLong("initiator_acc"));
			
		}
		
		} catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
		}
		return transcation;
	}

	@Override
	public List<Transaction> getThe30MostRecentTransactions() throws BusinessException {
		List<Transaction> result = null;
		
		try(Connection connection = PostgresqlConnection.getConnection()){
			
			String sql = "select * from my_bank_app.\"transaction\" order by trans_number desc limit 30";
			PreparedStatement preparedStatement = connection.prepareStatement(sql);
			ResultSet resultSet = preparedStatement.executeQuery();
			result = new ArrayList<Transaction>();
			while(resultSet.next()) {
				Transaction transaction = new Transaction();
				transaction.setAmount(resultSet.getDouble("amount"));
				transaction.setBalance_after_deposit(resultSet.getDouble("balance_after_deposit"));
				transaction.setBalance_after_withdraw(resultSet.getDouble("balance_after_withdraw"));
				transaction.setDeposit_to(resultSet.getLong("deposit_to"));
				transaction.setInitiator_acc(resultSet.getLong("initiator_acc"));
				transaction.setStatus(resultSet.getString("status"));
				transaction.setTime_completed(resultSet.getString("time_completed"));
				transaction.setTime_requested(resultSet.getString("time_requested"));
				transaction.setTrans_number(resultSet.getLong("trans_number"));
				transaction.setType(resultSet.getString("type"));
				transaction.setWithdraw_from(resultSet.getLong("withdraw_from"));
				
				result.add(transaction);
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		} catch (ClassNotFoundException e) {
			log.info(e);
			log.info("connection fail");
		} catch (SQLException e) {
			log.info(e);
			log.info("sql command fail");
		}
		return result;
	}

}
