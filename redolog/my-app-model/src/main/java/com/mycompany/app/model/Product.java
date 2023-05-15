/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycompany.app.model;

import com.gigaspaces.annotation.pojo.*;
import javax.persistence.*;

@Entity
@Table(name = "Product")
@SpaceClass
public class Product implements java.io.Serializable {
    private Integer id;
    private String name;
    private Integer price;

    public Product() {
    }

    public Product(Integer id, String name, Integer price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    @Id
    @SpaceId
    @SpaceIndex(unique = true)
    @Column(name = "Id")
    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }

    //@SpaceIndex
    @Column(name = "Name")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }


   // @SpaceIndex(type=SpaceIndexType.ORDERED)
    @Column(name = "Price")
    public Integer getPrice() {
        return price;
    }
    public void setPrice(Integer price) {
        this.price = price;
    }
}
