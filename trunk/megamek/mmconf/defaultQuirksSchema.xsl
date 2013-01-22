<?xml version="1.0" encoding="UTF-8"?>

<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">
    <!-- unit elements -->
    <xs:element name="chassis" type="xs:string"/>
    <xs:element name="model" type="xs:string"/>
    <xs:element name="quirk" type="xs:string"/>
    <!-- /unit elements -->
    
    <!-- weaponQuirk elements -->
    <xs:element name="weaponQuirkName" type="xs:string"/>
    <xs:element name="location" type="xs:string"/>
    <xs:element name="slot" type="xs:int"/>
    <xs:element name="weaponName" type="xs:string"/>
    <!-- /weaponQuirk elements -->

    <xs:element name="defaultQuirks">
        <xs:complexType>
            <xs:sequence>
                <xs:element maxOccurs="unbounded" ref="unit"/>
            </xs:sequence>
        </xs:complexType>
        <xs:unique name="testUnique">
            <xs:selector xpath="unit"/>
            <xs:field xpath="chassis"/>
            <xs:field xpath="model"/>
        </xs:unique>
    </xs:element>

    <xs:element name="unit">
        <xs:complexType>
            <xs:sequence>
                <xs:element maxOccurs="1" ref="chassis"/>
                <xs:element maxOccurs="1" ref="model"/>
                <xs:element minOccurs="0" maxOccurs="unbounded" ref="quirk"/>
                <xs:element minOccurs="0" maxOccurs="unbounded" ref="weaponQuirk"/>
            </xs:sequence>
        </xs:complexType>
        <xs:unique name="uniqueQuirk">
            <xs:selector xpath="quirk"/>
            <xs:field xpath="quirk"/>
        </xs:unique>
        <xs:unique name="uniqueWeaponQuirk">
            <xs:selector xpath="weaponQuirk"/>
            <xs:field xpath="weaponQuirkName"/>
            <xs:field xpath="location"/>
            <xs:field xpath="slot"/>
        </xs:unique>
    </xs:element>

    <xs:element name="weaponQuirk">
        <xs:complexType>
            <xs:all>
                <xs:element ref="weaponQuirkName"/>
                <xs:element ref="location"/>
                <xs:element ref="slot"/>
                <xs:element ref="weaponName"/>
            </xs:all>
        </xs:complexType>
    </xs:element>
</xs:schema>
