package com.zimbra.cs.offline.jsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.offline.common.OfflineConstants;
import com.zimbra.cs.offline.common.OfflineConstants.SyncStatus;

public class ConsoleBean extends PageBean {
    public class AccountSummary {
        private String id;
        private String type;
        private String flavor;
        private String name;
        private String email;
        private long lastSync;
        private SyncStatus status = SyncStatus.unknown;
        private String errorCode;
        private String errorMsg;
        private String exception;
        private boolean isFirst;

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getFlavor() {
            return flavor;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public Date getLastSync() {
            return lastSync == 0 ? null : new Date(lastSync);
        }

        public SyncStatus getSyncStatus() {
            return status;
        }
        
        public String getErrorCode() {
        	return errorCode;
        }
        
        public String getErrorMsg() {
        	return errorMsg;
        }
        
        public String getException() {
        	return exception;
        }
        
        public String getUserFriendlyErrorMessage() {
        	if (errorCode == null)
        		errorCode = "offline.UNEXPECTED";
        	
        	String msg = getMessage("client." + errorCode, false);
        	if (msg == null)
        		msg = getMessage("exception." + errorCode, false);
        	if (msg == null)
        		msg = getMessage("exception.offline.UNEXPECTED", false);
        	return msg;
        }
        
        public boolean isStatusUnknown() {
            return status == SyncStatus.unknown;
        }

        public boolean isStatusOffline() {
            return status == SyncStatus.offline;
        }

        public boolean isStatusOnline() {
            return status == SyncStatus.online;
        }

        public boolean isStatusRunning() {
            return status == SyncStatus.running;
        }

        public boolean isStatusAuthFailed() {
            return status == SyncStatus.authfail;
        }

        public boolean isStatusError() {
            return status == SyncStatus.error;
        }

        public boolean isFirst() {
            return isFirst;
        }
    }

    // only set for promoting
    private String accountId;
    private String verb;

    private AccountSummary[] savedAccounts;

    public ConsoleBean() {}

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public AccountSummary[] getAccounts() throws ServiceException {
        JspProvStub stub = JspProvStub.getInstance();

        String[] order = null;
        if (accountId != null && verb == null) {
            order = stub.promoteAccount(accountId);
            accountId = null;
        } else if (savedAccounts != null)
            return savedAccounts;
        else
            order = stub.getAccountsOrder();

        List<AccountSummary> sums = new ArrayList<AccountSummary>();
        List<Account> accounts = stub.getOfflineAccounts();
        for (Account account : accounts) {
            AccountSummary sum = new AccountSummary();
            sum.id = account.getId();
	    sum.type = "zimbra";
            sum.flavor = account.getAttr(OfflineConstants.A_offlineAccountFlavor);
	    if (sum.flavor == null)
		sum.flavor = "Zimbra";
            sum.name = account.getAttr(Provisioning.A_zimbraPrefLabel);
            sum.name = sum.name != null ? sum.name :
                account.getAttr(OfflineConstants.A_offlineAccountName);
            sum.email = account.getName();
            sum.lastSync = account.getLongAttr(OfflineConstants.A_offlineLastSync, 0);
            String status = account.getAttr(OfflineConstants.A_offlineSyncStatus);
            sum.status = status == null ? SyncStatus.unknown : SyncStatus.valueOf(status);
            sum.errorCode = account.getAttr(OfflineConstants.A_offlineSyncStatusErrorCode);
            sum.errorMsg = account.getAttr(OfflineConstants.A_offlineSyncStatusErrorMsg);
            sum.exception = account.getAttr(OfflineConstants.A_offlineSyncStatusException);
            sums.add(sum);
        }
        List<DataSource> dataSources = stub.getOfflineDataSources();
        for (DataSource ds : dataSources) {
            AccountSummary sum = new AccountSummary();
            sum.id = ds.getAccountId();
	    sum.type = ds.getType().toString();
            sum.flavor = ds.getAttr(OfflineConstants.A_offlineAccountFlavor);
	    if (sum.flavor == null) {
		String domain = ds.getAttr(OfflineConstants.A_zimbraDataSourceDomain, null);
		if (sum.type.equals("live"))
		    sum.flavor = "Live";
		else if (sum.type.equals("pop3"))
		    sum.flavor = "Pop";
		else if (domain.equals(AmailBean.Domain))
		    sum.flavor = "AOL";
		else if (domain.equals(GmailBean.Domain))
		    sum.flavor = "Gmail";
		else if (domain.equals(MmailBean.Domain))
		    sum.flavor = "MSE";
		else if (domain.equals(YmailBean.Domain))
		    sum.flavor = "YMP";
		else
		    sum.flavor = "Imap";
	    }
            sum.name = ds.getName();
            sum.email = ds.getEmailAddress();
            sum.lastSync = ds.getLongAttr(OfflineConstants.A_zimbraDataSourceLastSync, 0);
            String status = ds.getAttr(OfflineConstants.A_zimbraDataSourceSyncStatus);
            sum.status = status == null ? SyncStatus.unknown : SyncStatus.valueOf(status);
            sum.errorCode = ds.getAttr(OfflineConstants.A_zimbraDataSourceSyncStatusErrorCode);
            sums.add(sum);
        }

        final String[] finalOrder = order;
        final AccountSummary[] sumArray = new AccountSummary[sums.size()];
        sums.toArray(sumArray);

        if (sumArray.length > 0) {
            Arrays.sort(sumArray, new Comparator<AccountSummary>() {
                public int compare(AccountSummary o1, AccountSummary o2) {
                    int index1 = sumArray.length, index2 = sumArray.length;
                    for (int i = 0; i < finalOrder.length; ++i) {
                        if (o1.id.equals(finalOrder[i]))
                            index1 = i;
                        else if (o2.id.equals(finalOrder[i]))
                            index2 = i;
                    }
                    return index1 - index2;
                }
            });
            sumArray[0].isFirst = true;
        }
        savedAccounts = sumArray;
        return sumArray;
    }
}
