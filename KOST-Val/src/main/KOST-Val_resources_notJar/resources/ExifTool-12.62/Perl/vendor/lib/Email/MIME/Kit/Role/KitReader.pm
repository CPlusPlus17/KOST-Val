package Email::MIME::Kit::Role::KitReader 3.000007;
# ABSTRACT: things that can read kit contents

use v5.20.0;
use Moose::Role;
with 'Email::MIME::Kit::Role::Component';

#pod =head1 IMPLEMENTING
#pod
#pod This role also performs L<Email::MIME::Kit::Role::Component>.
#pod
#pod Classes implementing this role must provide a C<get_kit_entry> method.  It will
#pod be called with one parameter, the name of a path to an entry in the kit.  It
#pod should return a reference to a scalar holding the contents (as octets) of the
#pod named entry.  If no entry is found, it should raise an exception.
#pod
#pod A method called C<get_decoded_kit_entry> is provided.  It behaves like
#pod C<get_kit_entry>, but assumes that the entry for that name is stored in UTF-8
#pod and will decode it to text before returning.
#pod
#pod =cut

requires 'get_kit_entry';

sub get_decoded_kit_entry {
  my ($self, $name) = @_;
  my $content_ref = $self->get_kit_entry($name);

  require Encode;
  my $decoded = Encode::decode('utf-8', $$content_ref);
  return \$decoded;
}

no Moose::Role;
1;

__END__

=pod

=encoding UTF-8

=head1 NAME

Email::MIME::Kit::Role::KitReader - things that can read kit contents

=head1 VERSION

version 3.000007

=head1 PERL VERSION

This library should run on perls released even a long time ago.  It should work
on any version of perl released in the last five years.

Although it may work on older versions of perl, no guarantee is made that the
minimum required version will not be increased.  The version may be increased
for any reason, and there is no promise that patches will be accepted to lower
the minimum required perl.

=head1 IMPLEMENTING

This role also performs L<Email::MIME::Kit::Role::Component>.

Classes implementing this role must provide a C<get_kit_entry> method.  It will
be called with one parameter, the name of a path to an entry in the kit.  It
should return a reference to a scalar holding the contents (as octets) of the
named entry.  If no entry is found, it should raise an exception.

A method called C<get_decoded_kit_entry> is provided.  It behaves like
C<get_kit_entry>, but assumes that the entry for that name is stored in UTF-8
and will decode it to text before returning.

=head1 AUTHOR

Ricardo Signes <rjbs@cpan.org>

=head1 COPYRIGHT AND LICENSE

This software is copyright (c) 2023 by Ricardo Signes.

This is free software; you can redistribute it and/or modify it under
the same terms as the Perl 5 programming language system itself.

=cut
