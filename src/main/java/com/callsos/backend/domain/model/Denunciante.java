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
    private String documento;  // nullable — seed data no lo tiene; registro nuevo sí lo exige
    private String origen;
    private String telefono;
    private String correo;
    private String tokenFcm;   // nullable — para notificaciones push
 
    /** Constructor completo — incluye documento y tokenFcm. */
    public Denunciante(String id, String nombre, String documento, String origen,
                       String telefono, String correo, String tokenFcm) {
        this.id        = id;
        this.nombre    = nombre;
        this.documento = documento;
        this.origen    = origen;
        this.telefono  = telefono;
        this.correo    = correo;
        this.tokenFcm  = tokenFcm;
    }

    /**
     * Constructor previo a este fix (6 parámetros, sin documento) — usado
     * por IncidenteRepositoryMySQL. documento queda null.
     *
     * NOTA: no se agregó un constructor de 6 parámetros "con documento,
     * sin tokenFcm" porque colisionaría con este (misma firma de tipos
     * String×6 — Java resuelve sobrecarga por tipo, no por nombre de
     * parámetro). RegistrarDenuncianteService usa el constructor completo
     * de 7 parámetros de arriba, pasando null explícito en tokenFcm.
     */
    public Denunciante(String id, String nombre, String origen,
                       String telefono, String correo, String tokenFcm) {
        this(id, nombre, null, origen, telefono, correo, tokenFcm);
    }
 
    /** Constructor de compatibilidad sin documento ni tokenFcm (ambos quedan null). */
    public Denunciante(String id, String nombre, String origen,
                       String telefono, String correo) {
        this(id, nombre, null, origen, telefono, correo, null);
    }
 
    public String getId()        { return id; }
    public String getNombre()    { return nombre; }
    public String getDocumento() { return documento; }
    public String getOrigen()    { return origen; }
    public String getTelefono()  { return telefono; }
    public String getCorreo()    { return correo; }
    public String getTokenFcm()  { return tokenFcm; }
 
    public boolean tieneTokenFcm() {
        return tokenFcm != null && !tokenFcm.isBlank();
    }
}