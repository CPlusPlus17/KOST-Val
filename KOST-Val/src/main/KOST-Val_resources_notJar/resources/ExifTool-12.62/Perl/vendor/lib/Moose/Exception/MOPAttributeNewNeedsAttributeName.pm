package Moose::Exception::MOPAttributeNewNeedsAttributeName;
our $VERSION = '2.2203';

use Moose;
extends 'Moose::Exception';
with 'Moose::Exception::Role::ParamsHash';

has 'class' => (
    is       => 'ro',
    isa      => 'Str',
    required => 1
);

sub _build_message {
    "You must provide a name for the attribute";
}

__PACKAGE__->meta->make_immutable;
1;