<?xml version="1.0" encoding="UTF-8"?>

<!-- Schema for defining images and colors to be used for different UI elements in Megamek -->
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">

    <!-- Defines the images that will be used in a border -->
    <xs:element name="border">
      <!-- Corner images   -->
      <xs:element name="corner_top_left" type="xs:string"/>
      <xs:element name="corner_top_right" type="xs:string"/>    
      <xs:element name="corner_bottom_left" type="xs:string"/>    
      <xs:element name="corner_bottom_right" type="xs:string"/>        
      <!-- Border edges -->
      <xs:complexType>
        <xs:sequence>
          <xs:element minOccurs="4" maxOccurs="4" ref="edge"/>
        </xs:sequence>
      </xs:complexType>      
    </xs:element>

    <!-- Defines the images that will be used in an edge -->
    <xs:element name="edge">      
      <!-- A sequence of image/tiled pairs -->
      <xs:element name="edgeName" type=xs:"string"/>
      <xs:complexType>
        <xs:sequence>
          <xs:element maxOccurs="unbounded" ref="edgeIcon"/>
        </xs:sequence>     
      </xs:complexType>
    </xs:element>

    <!-- Defines the images and whether it should be tiled or not -->
    <xs:element name="edgeIcon">    
      <xs:element name="icon" type="xs:string"/>
      <xs:element name="tiled" type="xs:string"/>    
    </xs:element>  


    <xs:element name="UI_Element">
      <!-- The name of the UI element -->
      <xs:element name="name" type="xs:string"/>    
      <!-- Specification of border images -->
      <xs:complexType>
        <xs:element  ref="border"/>
      </xs:complexType>
      <!-- Specification of background images -->
      <xs:sequence> 
        <xs:element name="background_image" type="xs:string"/>
      </xs:sequence>
    </xs:element>

</xs:schema>
