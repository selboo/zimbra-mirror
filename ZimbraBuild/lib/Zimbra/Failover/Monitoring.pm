# 
# ***** BEGIN LICENSE BLOCK *****
# Version: ZPL 1.1
# 
# The contents of this file are subject to the Zimbra Public License
# Version 1.1 ("License"); you may not use this file except in
# compliance with the License. You may obtain a copy of the License at
# http://www.zimbra.com/license
# 
# Software distributed under the License is distributed on an "AS IS"
# basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
# the License for the specific language governing rights and limitations
# under the License.
# 
# The Original Code is: Zimbra Collaboration Suite.
# 
# The Initial Developer of the Original Code is Zimbra, Inc.
# Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
# All Rights Reserved.
# 
# Contributor(s):
# 
# ***** END LICENSE BLOCK *****
# 
package Zimbra::Failover::Monitoring;

require Exporter;
@ISA = qw(Exporter);
@EXPORT_OK = qw(isServiceAvailable);

use strict;
use DBI;
use SOAP::Lite;
use Zimbra::Failover::LDAP;
use Zimbra::Failover::Config;
use Zimbra::Failover::Control qw(zmcontrol isServiceRunning);
use Zimbra::Failover::SoapToTomcat;

sub isServiceAvailable() {
    #return zmcontrol('status');
    #return isServiceRunning();
    return checkAll();
}

sub checkAll() {
    my $ldap = checkLDAP();
    my $db = checkDB();
    my $tomcat = checkTomcat();
    my $network = checkServiceIP();
    if ($ldap && $db && $tomcat && $network) {
        return 1;
    }
    my $msg = sprintf("STATUS: ldap=%s, db=%s, tomcat=%s, network=%s\n",
                      $ldap ? 'up' : 'down',
                      $db ? 'up' : 'down',
                      $tomcat ? 'up' : 'down',
                      $network ? 'up' : 'down');
    print $msg;
    return 0;
}

#
# Tries to connect to LDAP server and search for default admin account entry.
# Returns 1 if successful, 0 if error.
#
sub checkLDAP() {
    my $ldap = new Zimbra::Failover::LDAP();
    $ldap->bind() or return 0;
    my %conf = ();
    my $success = $ldap->getGlobalServerConfig(\%conf);
    $ldap->unbind();
    return $success;

    my $attr = 'uid';
    my $search = $ldap->getDirContext()->search(
        base => 'cn=admins,cn=zimbra',
        scope => 'one',
        filter => "(&($attr=zimbra)(objectClass=zimbraAccount))",
        attrs => [$attr]
    );
    if ($search->code()) {
        print STDERR "LDAP search failed: " . $search->error() . "\n";
        $ldap->unbind();
        return 0;
    }
    my $entry = $search->entry(0);
    if (!defined($entry)) {
        print STDERR "LDAP search returned no entry\n";
        $ldap->unbind();
        return 0;
    }
    my $uid = $entry->get_value($attr);
    if (!defined($uid)) {
        print STDERR "LDAP search result missing $attr attribute\n";
       $ldap->unbind();
        return 0;
    }

    $ldap->unbind();
    return 1;
}

#
# Tries to connect to database and run a simple query.
# Returns 1 if successful, 0 if error.
#
sub checkDB() {

    my $db = Zimbra::Failover::Db->connect();
    if (!$db) {
        print STDERR "Unable to connect to database\n";
        return 0;
    }

    my $foo = 'foo';
    my $stmt = "SELECT '$foo' AS TEST";
    my $sth = $db->{CONN}->prepare($stmt);
    my $rv = $sth->execute();
    if (!$rv) {
        print STDERR "Unable to execute query: $DBI::errstr\n";
        $db->disconnect();
        return 0;
    }
    my $value;
    my @data;
    if (@data = $sth->fetchrow_array()) {
        $value = $data[0];
    } else {
        print STDERR "Unable to fetch row: $DBI::errstr\n";
        $sth->finish();
        $db->disconnect();
        return 0;
    }
    $sth->finish();
    $db->disconnect();

    if ($value ne $foo) {
        print STDERR
            "Retrieved value ($value) is different from original ($foo).\n";
        return 0;
    }
    return 1;
}

sub checkTomcat() {
    my $resp = Zimbra::Failover::SoapToTomcat::checkHealth();
    if (!defined($resp)) {
        print STDERR "No positive ping response from Tomcat\n";
        return 0;
    }
    my $healthy = $resp->attr('healthy');
    return $healthy ? 1 : 0;
}

#
# Checks connectivity to the outside world, by pinging the router between
# service IP and end users.
#
sub checkServiceIP() {
    my $role = Zimbra::Failover::Config::getCurrentRole();
    if (defined($role) && $role eq 'slave') {
        # Don't check on slave host, which doesn't own the service IP.
        return 1;
    }
    my $serviceIP = Zimbra::Failover::Config::getServiceIP();
    my $routerIP = Zimbra::Failover::Config::getRouterIP();
    if (!defined($serviceIP) || !defined($routerIP)) {
        print STDERR
            "No local and/or router IP defined; Network check skipped.\n";
        return 1;
    }
    return Zimbra::Failover::IPUtil::isPingable($serviceIP, $routerIP);
}

1;
