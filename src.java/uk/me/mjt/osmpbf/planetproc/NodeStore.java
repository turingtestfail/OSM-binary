/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.me.mjt.osmpbf.planetproc;


public interface NodeStore {

    SimpleNode get(long index);

    void put(SimpleNode node);
    
}
