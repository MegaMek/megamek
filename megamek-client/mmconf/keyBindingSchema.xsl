<?xml version="1.0" encoding="UTF-8"?>

<!-- Schema for defining images and colors to be used for different UI elements in Megamek -->
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" elementFormDefault="qualified">

    <!-- Defines the images that will be used in a border -->
    <xs:element name="KeyBind">
      <xs:element name="command" type="xs:string"/>    
      <!-- Boolean flag that determines whether this command should be repeated when the key is held down -->
      <!-- Defines what key is being bound, using defined values in awt.event.KeyEvent -->
      <xs:element name="keyCode" type="xs:integer"/>
      <!-- Defines any modifiers for they key (shift,ctrl, etc), using defined values in awt.event.KeyEvent -->
      <xs:element name="modifier" type="xs:integer"/>  
      <!-- The string command that will be executed when this key is pressed, for a list of commands see megamek.client.ui.swing.util.KeyBindCommand -->  
      <xs:element name="isRepeatable" type="xs:byte"/>        
    </xs:element>

</xs:schema>
