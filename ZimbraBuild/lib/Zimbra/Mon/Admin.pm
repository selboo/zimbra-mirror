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
#!/usr/bin/perl

package Zimbra::Mon::Admin;

use strict;

use Zimbra::Mon::Logger;
use Zimbra::Mon::Cluster;
use Zimbra::Mon::Host;
use Zimbra::Mon::shortInfo;
use SOAP::Lite;

use DBI;
use Zimbra::Mon::Account;

require Exporter;

my @ISA = qw(Exporter);

# This is PER HOST, so keep it low-ish.
my $MAX_EVENTS_RETURNED = 20;

#my @EXPORT = qw (GetHostListRequest);

# TODO MEM - for control commands, (start) - open FIFO.

sub new {
	my ($class) = @_;
	return $class if ref($class);

	my $self = bless {}, $class;

	# TODO MEM - not sure if I need to close this when done
	$::Cluster->openFifo();

	#Zimbra::Mon::Logger::Log( "debug", "Created admin" );
	return $self;
}

sub GetLocalHostNameRequest {
	my $self = shift->new;
	#Zimbra::Mon::Logger::Log( "debug", "GetLocalHostNameRequest" );

	return $::Cluster->{LocalHost};
}

sub GetHostListRequest {

	my $self = shift->new;
	#Zimbra::Mon::Logger::Log( "debug", "GetHostListRequest" );

	return $::Cluster->getClusterHosts();
}

sub GetServicesRequest {

	my $self = shift->new;
	my $hn   = shift;
	#Zimbra::Mon::Logger::Log( "debug", "GetServicesRequest: $hn" );

	if ( $hn eq $::Cluster->{LocalHost}->{name} ) {
		my $ret = $::Cluster->getLocalServices();

		#		Zimbra::Mon::Logger::Log ("debug","getLocalServices() returned");
		#		foreach my $r(@{$ret})
		#		{
		#				Zimbra::Mon::Logger::Log ("debug",$r->prettyPrint());
		#		}
		return $ret;
	}
	else {

		my $H   = $self->getHostByName($hn);
		my $ret = undef;
		if ( !defined($H) ) {
			#Zimbra::Mon::Logger::Log( "debug", "GetServicesRequest: Host $hn not found" );
			return undef;
		}
		eval {
			my $resp = SOAP::Lite->proxy( "http://$H->{ip}:$::controlport/",
				timeout => 10 )
			  ->uri("http://$H->{ip}:$::controlport/Zimbra::Mon::Admin")
			  ->GetServicesRequest( $H->{name} );

			if ( !defined $resp->result ) {
				Zimbra::Mon::Logger::Log( "err",
"Error contacting $H->{ip} ($H->{name}): No response from server: "
					  . $resp->faultstring );
			}

			$ret = $resp->result;
			Zimbra::Mon::Logger::Log( "debug",
				"Response from $H->{ip} ($H->{name}): " . ref($ret) );
		};
		if ($@) {
			Zimbra::Mon::Logger::Log( "err",
				"Error contacting $H->{ip} ($H->{name}): $@" );
		}
		return $ret;

	}
}

sub GetShortInfoRequest {

	my $self = shift->new;
	my $hn   = shift;
	Zimbra::Mon::Logger::Log( "debug", "GetShortInfoRequest: $hn" );
	my @si = ();

	if ( $hn eq $::Cluster->{LocalHost}->{name} ) {
		my $ret = $::Cluster->getLocalShortInfo();
		$ret->{ServiceInfo} = $::Cluster->getLocalServiceInfo();
		push @si, $ret;
		#Zimbra::Mon::Logger::Log( "debug", "Returning info for $hn: " . ref($ret) );
		return @si;
	}
	elsif ( $hn eq "All Hosts" ) {
		my @hosts = $::Cluster->getClusterHosts();
		#Zimbra::Mon::Logger::Log( "debug", "GetShortInfoRequest for all hosts" );
		foreach (@hosts) {
			my @ret = $self->GetShortInfoRequest( $_->{name} );
			if ( defined @ret[0] ) {

			  #Zimbra::Mon::Logger::Log ("info","Got info for $_->{name}: ".ref(@ret[0]));
				push @si, @ret[0];
			}
		}
		return @si;
	}
	else {
		my $H   = $self->getHostByName($hn);
		my $ret = undef;
		if ( !defined($H) ) {
			Zimbra::Mon::Logger::Log( "err", "GetShortInfoRequest: Host $hn not found" );
			return undef;
		}
		eval {
			my $resp = SOAP::Lite->proxy( "http://$H->{ip}:$::controlport/",
				timeout => 10 )
			  ->uri("http://$H->{ip}:$::controlport/Zimbra::Mon::Admin")
			  ->GetShortInfoRequest( $H->{name} );

			if ( !defined $resp->result ) {
				Zimbra::Mon::Logger::Log( "err",
"Error contacting $H->{ip} ($H->{name}): No response from server: "
					  . $resp->faultstring );
			}

			$ret = $resp->result;
			#Zimbra::Mon::Logger::Log( "debug",
				#"Response from $H->{ip} ($H->{name}): " . ref($ret) );
		};
		if ($@) {
			Zimbra::Mon::Logger::Log( "err",
				"Error contacting $H->{ip} ($H->{name}): $@" );
		}
		return $ret;
	}
}

sub GetServiceInfoRequest {
	my $self = shift->new;
	my $hn   = shift;
	#Zimbra::Mon::Logger::Log( "debug", "GetServiceInfoRequest: $hn" );

	my @si = ();

	if ( $hn eq $::Cluster->{LocalHost}->{name} ) {
		my $ret = $::Cluster->getLocalServiceInfo();
		push @si, $ret;
		return @si;
	}
	elsif ( $hn eq "All Hosts" ) {
		my @hosts = $::Cluster->getClusterHosts();
		#Zimbra::Mon::Logger::Log( "debug", "GetServiceInfoRequest for all hosts" );
		foreach (@hosts) {
			my @ret = $self->GetServiceInfoRequest( $_->{name} );
			if ( defined @ret[0] ) {

			  #Zimbra::Mon::Logger::Log ("info","Got info for $_->{name}: ".ref(@ret[0]));
				push @si, @ret[0];
			}
		}
		return @si;
	}
	else {
		my $H   = $self->getHostByName($hn);
		my $ret = undef;
		if ( !defined($H) ) {
			#Zimbra::Mon::Logger::Log( "debug",
			#	"GetServiceInfoRequest: Host $hn not found" );
			return undef;
		}
		eval {
			my $resp = SOAP::Lite->proxy( "http://$H->{ip}:$::controlport/",
				timeout => 10 )
			  ->uri("http://$H->{ip}:$::controlport/Zimbra::Mon::Admin")
			  ->GetServiceInfoRequest( $H->{name} );

			if ( !defined $resp->result ) {
				Zimbra::Mon::Logger::Log( "err",
"Error contacting $H->{ip} ($H->{name}): No response from server: "
					  . $resp->faultstring );
			}

			$ret = $resp->result;
			#Zimbra::Mon::Logger::Log( "debug",
				#"Response from $H->{ip} ($H->{name}): " . ref($ret) );
		};
		if ($@) {
			Zimbra::Mon::Logger::Log( "err",
				"Error contacting $H->{ip} ($H->{name}): $@" );
		}
		return $ret;
	}
}

sub ServiceControlRequest {

	my $self   = shift->new;
#	my $sn     = shift;
	my $hn     = shift;
	my $action = shift;

	Zimbra::Mon::Logger::Log( "info", "ServiceControlRequest: $action on $hn" );
	if ( $hn eq $::Cluster->{LocalHost}->{name} ) {
		my $ret = $::Cluster->controlLocalService( uc($action) );
		return $ret;
	}
	else {
		my $H   = $self->getHostByName($hn);
		my $ret = undef;
		if ( !defined($H) ) {
			Zimbra::Mon::Logger::Log( "info",
				"ServiceControlRequest: Host $hn not found" );
			return undef;
		}
		eval {
			my $resp = SOAP::Lite->proxy( "http://$H->{ip}:$::controlport/",
				timeout => 10 )
			  ->uri("http://$H->{ip}:$::controlport/Zimbra::Mon::Admin")
			  ->ServiceControlRequest( $H->{name}, $action );

			if ( !defined $resp->result ) {
				Zimbra::Mon::Logger::Log( "err",
"Error contacting $H->{ip} ($H->{name}): No response from server: "
					  . $resp->faultstring );
			}

			$ret = $resp->result;
			#Zimbra::Mon::Logger::Log( "debug",
				#"Response from $H->{ip} ($H->{name}): " . ref($ret) );
		};
		if ($@) {
			Zimbra::Mon::Logger::Log( "err",
				"Error contacting $H->{ip} ($H->{name}): $@" );
		}
		return $ret;
	}
}

# sub GetLocalEventsRequest {
# 	my $self       = shift->new;
# 	my $firstEvent = shift;
# 	my $lastEvent  = shift;
# 
# 	#Zimbra::Mon::Logger::Log( "debug", "GetEventsRequest: $firstEvent, $lastEvent" );
# 	my $ret = $::Cluster->getEventList( $firstEvent . $lastEvent );
# 
# 	# TODO MEM - this always assumes a zero offset, which is probably busted.
# 	if ( $firstEvent < 0 ) { $firstEvent = 0; }
# 	if ( $lastEvent > $#{ $ret->{Events} } ) {
# 		$lastEvent = $#{ $ret->{Events} };
# 	}
# 
# 	my $range = $lastEvent - $firstEvent;
# 
# 	return $ret;
# }

# sub GetEventsRequest {
# 	my $self       = shift->new;
# 	my $filter	= shift;
# 
# 	# Filter:
# 	#	starttime
# 	#	endtime
# 	#	hostname
# 	#	fetchRef
# 	
# 	if (!defined $filter->{hostname} || $filter->{hostname} eq "")
# 	{ 
# 		Zimbra::Mon::Logger::Log("err", "malformed GetEventsRequest (null host)");
# 		return undef;
# 	}
# 	
# 	Zimbra::Mon::Logger::Log("info", "GetEventsRequest for $filter->{hostname} $filter->{starttime} - $filter->{endtime}");
# 	
# 	my $fetchRef = '';
# 	
## 	if (! defined $filter->{fetchRef} || $filter->{fetchRef} eq "") {
## 		# Generate new ref here.
#		$fetchRef = $::Cluster->newFetchRef($filter);
#	} else {
#		# For an existing ref, get what's been returned
#		($filter->{low}, $filter->{high}) = $::Cluster->getFetchRef($filter);
#	}
# 	
# 	my $ev;
# 	
# 	my $ret;
# 	$ret->{Events} = ();
# 	
# 		my @hl;
# 		if ($filter->{hostname} ne '*') {
# 			push @hl, $self->getHostByName($filter->{hostname});
# 		} else {
# 			@hl = $::Cluster->getClusterHosts();
# 		}
# 		foreach my $h (@hl) {
# 			if (defined ($h)) {
# 				Zimbra::Mon::Logger::Log("info", "GetEventsRequest for $h->{name}");
# 				$ev = $self->getEventsFromHost( $filter, $h );
# 				splice( @{ $ret->{Events} }, @{ $ret->{Events} }, 0, @{ $ev->{Events} } );
# 			} else {
# 				Zimbra::Mon::Logger::Log("err", "Unknown host $h");
# 			}
# 		}
# 
# 	return $ret;
# 		
# }

# sub getEventsFromHost {
# 	my $self = shift;
# 	my $filter = shift;
# 	
# 	my $H   = shift;
# 	my $firstEvent = $filter->{starttime};
# 	my $lastEvent = $filter->{endtime};
# 	Zimbra::Mon::Logger::Log( "info", "getEventsFromHost $H->{name} on $::Cluster->{LocalHost}->{name}" );
# 
# 	my $ret;
# 
# 	if ($H->{name} eq $::Cluster->{LocalHost}->{name}) {
# 		$ret = $::Cluster->getEventList( $firstEvent , $lastEvent )
# 	} else {	
# 		eval {
# 			my $resp =
# 			  SOAP::Lite->proxy( "http://$H->{ip}:$::controlport/", timeout => 10 )
# 			  ->uri("http://$H->{ip}:$::controlport/Zimbra::Mon::Admin")
# 			  ->GetLocalEventsRequest( $firstEvent, $lastEvent );
# 	
# 			if ( !defined $resp->result ) {
# 				Zimbra::Mon::Logger::Log( "err",
# 	"Error contacting $H->{ip} ($H->{name}): No response from server: "
# 					  . $resp->faultstring );
# 				return undef;
# 			}
# 	
# 			$ret = $resp->result;
# 			#Zimbra::Mon::Logger::Log( "debug",
# 				#"Response from $H->{ip} ($H->{name}): " . ref($ret) );
# 		};
# 		if ($@) {
# 			Zimbra::Mon::Logger::Log( "err", "Error contacting $H->{ip} ($H->{name}): $@" );
# 		}
# 	}
# 	return $ret;
# }

sub AddHostRequest {
	my $self     = shift;
	my $hostName = shift;
	my $hostIp   = shift;

	Zimbra::Mon::Logger::Log( "debug", "AddHostRequest: $hostName, $hostIp" );
	my $ret = $::Cluster->addHost( $hostName, $hostIp );
	my ($n, $i, $p) = split (' ', $ret, 3);
	return new host ($n, $i, $p);
}

sub RemoveHostRequest {
	my $self     = shift;
	my $hostName = shift;
	my $hostIp   = shift;

	Zimbra::Mon::Logger::Log( "info", "RemoveHostRequest: $hostName, $hostIp" );
	my $ret = $::Cluster->removeHost( $hostName, $hostIp );
	return $ret;
}

sub updateClusterInfoRequest {
	my $self     = shift;
	my $sender   = shift;
	my $hostlist = shift;
	Zimbra::Mon::Logger::Log( "info", "updateClusterInfoRequest:" );

	$::Cluster->updateClusterInfo( $sender, $hostlist );
}

sub getHostByName {
	my $self = shift;
	my $hn   = shift;

	return $::Cluster->getHostByName($hn);
#	my @h = $::Cluster->getClusterHosts();
#	foreach ( @h ) {
#		if ( $_->{name} eq $hn ) { return $_; }
#	}
#	return undef;
}

sub SearchUsersRequest
{
	#TODO MEM - put all db access in main process, for persistent connection?
	my $self = shift;
	my $searchString = shift;
	
	Zimbra::Mon::Logger::Log ("err", "SearchUsersRequest: $searchString");
	
	my $data_source="dbi:mysql:database=zimbra;mysql_read_default_file=/opt/zimbra/conf/my.cnf";
	my $username="zimbra";
	my $password = `zmlocalconfig -s -m nokey zimbra_mysql_password`;
	chomp $password;

	my $dbh = DBI->connect($data_source, $username, $password);
	
	if (!$dbh) { 
		Zimbra::Mon::Logger::Log ("err", "DB: Can't connect to $data_source: $DBI::errstr");
		return undef;
	}

	# TODO - MEM - preprocess searchString for wildcards (*?)
	my $statement = "select * from account where name like '%".$searchString."%'";

	my $sth = $dbh->prepare($statement);

	if (!$sth->execute) {
		Zimbra::Mon::Logger::Log ("err", "DB: $sth->errstr");
		return undef;
	}

	my $hsh = $sth->fetchall_hashref("id");

	my @accounts = ();
	
	my $r;
	foreach $r (keys %{$hsh}) {
		my $row = $$hsh{$r};
		push (@accounts, Zimbra::Mon::Accountcreate($row));
#		foreach (keys %{$row}) {
#			Zimbra::Mon::Logger::Log ("debug", "DB: $r: $_ = $$row{$_}");
#		}
	}
	return @accounts;

}

sub NewUserRequest
{
	my $self = shift;
	my $userName = shift;
}

sub ReloadRequest
{
	my $self = shift;
	Zimbra::Mon::Logger::Log ("crit", "Reload request received");
	
	$::Cluster->sendFifo($::syntaxes{zimbrasyntax}{reload});
	return undef;
}

sub ShutDownRequest
{
	my $self = shift;
	Zimbra::Mon::Logger::Log ("crit", "Shutdown request received");
	
	$::Cluster->sendFifo($::syntaxes{zimbrasyntax}{shutdown});
	my $resp = $::Cluster->readFifo();
	return $resp;
}

1

