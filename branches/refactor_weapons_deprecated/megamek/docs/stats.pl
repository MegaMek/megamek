#!/usr/bin/perl

#
# stats.pl - The MegaMek serverlog.txt file analysis tool.
# Copyright 2004,2005 James Damour (suvarov454@users.sourceforge.net)
#
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by the Free
# Software Foundation; either version 2 of the License, or (at your option)
# any later version.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
# or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
# for more details.
#

#
# Usage: stats.pl filename1 [filename2 ...]
#

use strict;
my ( $filename );
my ( $line, @raw, %grand, %file, %try, %hit, %psr, %unc, %crit, %init );
my ( $target, $roll, $rest );

#
# Walk through the files.
#
foreach $filename ( @ARGV ) {
    open ( IN, "<$filename" ) || die "Could not open $filename.\n";

    # Mark the start of a new file.
    push @raw, "--$filename--";

    while ( $line = <IN> ) {

        # Clear the target and roll.
        $target = 0;
        $roll = 0;

        #
        # Go pattern matching.
        #

        # "/roll" command
        if ( $line =~ /has rolled \d, and \d for a total of (\d+)$/ ) {
            $roll = $1;
        }

        # initative
        elsif ( $line =~ /rolls a (\d+)( \/.*)?\.$/ ) {
            $roll = $1;
            $rest = $2;
            while ( $rest =~ / (\d+)( \/.*)?/ ) {
                push @raw, $roll;
                $grand{$roll}++;
                $file{$roll}++;
                $init{$roll}++;
                $roll = $1;
                $rest = $2;
            }
            $init{$roll}++;
        }

        # PSR roll
        elsif ( $line =~ /Roll \#.*; needs (\d+), rolls (\d+) :/ ) {
            $target = $1;
            $roll = $2;
            $try{$target}{$roll}++;
            $psr{$roll}++;
        }

        # piloting PSR
        elsif ( $line =~ /^Pilot.*must roll (\d+) to avoid damage; rolls (\d+) :/ ) {
            $target = $1;
            $roll = $2;
            $try{$target}{$roll}++;
            $psr{$roll}++;
        }

        # standing PSR
        elsif ( $line =~ /^Needs (\d+) .*, rolls (\d+) :/ ) {
            $target = $1;
            $roll = $2;
            $try{$target}{$roll}++;
            $psr{$roll}++;
        }

        # to-hit numbers.
        elsif ( $line =~ /; needs (\d+), rolls (\d+) :/ ) {
            $target = $1;
            $roll = $2;
            $try{$target}{$roll}++;
            $hit{$roll}++;
        }

        # Critical roll
        elsif ( $line =~ /Critical hit.*Roll = (\d+);/ ) {
            $roll = $1;
            $crit{$roll}++;
        }

        # Unconcious roll
        elsif ( $line =~ /Pilot.*needs a (\d+) to stay concious.  Rolls (\d+) :/ ) {
            $target = $1;
            $roll = $2;
            $try{$target}{$roll}++;
            $unc{$roll}++;
        }

        # Shutdown
        elsif ( $line =~ /needs a (\d+)\+ to avoid shutdown, rolls (\d+) :/ ) {
            $target = $1;
            $roll = $2;
            $try{$target}{$roll}++;
        }

        # Ammo explosion
        elsif ( $line =~ /needs a (\d+)\+ to avoid ammo explosion, rolls (\d+) :/ ) {
            $target = $1;
            $roll = $2;
            $try{$target}{$roll}++;
        }

        # If we got a roll, update the variables.
        if ( $roll ) {
            push @raw, $roll;
            $grand{$roll}++;
            $file{$roll}++;
        }

    } # Read the next line

    # Report and then empty the file's rolls
    report( "Total of Rolls for $filename", %file );
    %file = ();

} # Read from the next file

# Report the common stats
report( "Grand Total of Rolls", %grand );
report( "To-Hit Rolls", %hit );
report( "PSR Rolls", %psr );
report( "Conciousness Rolls", %unc );
report( "Critical Rolls", %crit );
report( "Initiative Rolls", %init );

# Report the rolls with targets.
print "Targeted Rolls:\t2\t3\t4\t5\t6\t7\t8\t9\t10\t11\t12\n";
foreach $target (2..12) {
    print "$target";
    foreach $roll (2..12) {
        print "\t$try{$target}{$roll}";
    }
    print "\n";
}
print "\n";

#  # Report the raw data.
#  print "Raw Data:\n";
#  foreach $roll ( @raw ) {
#      print "$roll\n";
#  }

1;

sub report {
    my ( $name, %stats ) = @_;
    my ( $roll );

    print "$name:\n";
    foreach $roll (2..12) {
        print "$roll\t$stats{$roll}\n";
    }
    print "\n";

}
