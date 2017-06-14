package de.spiritcroc.syncsettings;

import android.accounts.Account;

/**
 * Helper class to allow store screenshots without sharing too much confidential data
 */
class DemonstrationHelper {
    static final boolean ENABLED = false;

    static boolean isSyncWhiteListed(Account account, String authority) {
        return "maxmusterman@gmail.com".equals(account.name)
                && !"dont.want.this.provider.to.show".equals(authority);
    }
}
