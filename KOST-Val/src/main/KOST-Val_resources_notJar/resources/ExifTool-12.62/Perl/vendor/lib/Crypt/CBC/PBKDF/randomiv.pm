package Crypt::CBC::PBKDF::randomiv;

# This is for compatibility with early (pre v1.0) versions of OpenSSL
# THE KEYS GENERATED BY THIS ALGORITHM ARE INSECURE!!!
use strict;
use base 'Crypt::CBC::PBKDF';
use Digest::MD5 'md5';

# options:
# salt_len   => 8     default
# key_len    => 32    default
# iv_len     => 16    default

sub create {
    my $class = shift;
    my %options = @_;
    $options{salt_len} ||= 8;
    $options{key_len}  ||= 32;
    $options{iv_len}   ||= 16;
    return bless \%options,$class;
}

sub generate_hash {
    my $self = shift;
    my ($salt,$passphrase) = @_;
    my $desired_len = $self->{key_len};
    my $material = md5($passphrase);
    while (length($material) < $desired_len)  {
	$material .= md5($material);
    }
    substr($material,$desired_len) = '';
    $material     .= Crypt::CBC->_get_random_bytes($self->{iv_len});
    return $material;
}

1;