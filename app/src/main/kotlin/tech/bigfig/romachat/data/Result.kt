/*
 * Copyright (c) 2019 Vasily Kabunov
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>.
 */

package tech.bigfig.romachat.data

import android.net.Uri


/**
 * A generic class that holds a value with its loading status.
 */
data class Result<out T>(val status: ResultStatus, val data: T?, val pagination: Pagination?, val error: String?) {
    companion object {
        fun <T> success(data: T?): Result<T> {
            return Result(ResultStatus.SUCCESS, data, null, null)
        }

        fun <T> success(data: T?, pagination: Pagination?): Result<T> {
            return Result(ResultStatus.SUCCESS, data, pagination, null)
        }

        fun <T> error(error: String): Result<T> {
            return Result(ResultStatus.ERROR, null, null, error)
        }

        fun <T> loading(): Result<T> {
            return Result(ResultStatus.LOADING, null, null, null)
        }
    }
}

class Pagination(val prev: Uri?, val next: Uri?)

enum class ResultStatus {
    SUCCESS,
    ERROR,
    LOADING
}