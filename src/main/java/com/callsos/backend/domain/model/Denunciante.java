/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.callsos.backend.domain.model;

/**
 *
 * @author LENOVO
 */
/**
 * Ciudadano que reporta un incidente de seguridad.
 *
 * tokenFcm: token de Firebase Cloud Messaging necesario para
 * enviar notificaciones push a la app del denunciante.
 * Es nullable — un denunciante puede existir sin app instalada.
 */
public class Denunciante {
 
    private final String id;
    private String nombre;
    private String origen;
    private String telefono;
    private String correo;
    private String tokenFcm;   // nullable — para notificaciones push
 
    /** Constructor completo — incluye tokenFcm. */
    public Denunciante(String id, String nombre, String origen,
                       String telefono, String correo, String tokenFcm) {
        this.id       = id;
        this.nombre   = nombre;
        this.origen   = origen;
        this.telefono = telefono;
        this.correo   = correo;
        this.tokenFcm = tokenFcm;
    }
 
    /** Constructor de compatibilidad sin tokenFcm (lo deja null). */
    public Denunciante(String id, String nombre, String origen,
                       String telefono, String correo) {
        this(id, nombre, origen, telefono, correo, null);
    }
 
    public String getId()       { return id; }
    public String getNombre()   { return nombre; }
    public String getOrigen()   { return origen; }
    public String getTelefono() { return telefono; }
    public String getCorreo()   { return correo; }
    public String getTokenFcm() { return tokenFcm; }
 
    public boolean tieneNotificacionesActivas() {
        return tokenFcm != null && !tokenFcm.isBlank();
    }
}