/*
 *     This file is part of snapcast
 *     Copyright (C) 2014-2018  Johannes Pohl
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.badaix.snapcast.utils

import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/*
 * Copyright (C) 2012 The CyanogenMod Project
 *
 * * Licensed under the GNU GPLv2 license
 *
 * The text of the license can be found in the LICENSE file
 * or at https://www.gnu.org/licenses/gpl-2.0.txt
 */

object MD5 {
    private const val TAG = "MD5"

    fun checkMD5(md5: String?, updateFile: File?): Boolean {
        if (TextUtils.isEmpty(md5) || updateFile == null) {
            Log.e(TAG, "MD5 string empty or updateFile null")
            return false
        }

        val calculatedDigest = calculateMD5(updateFile)
        if (calculatedDigest == null) {
            Log.e(TAG, "calculatedDigest null")
            return false
        }

        Log.v(TAG, "Calculated digest: " + calculatedDigest)
        Log.v(TAG, "Provided digest: " + md5)

        return calculatedDigest.equals(md5, ignoreCase = true)
    }

    fun calculateMD5(updateFile: File?): String? {
        val `is`: InputStream?
        try {
            `is` = FileInputStream(updateFile)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Exception while getting FileInputStream", e)
            return null
        }
        try {
            return calculateMD5(`is`)
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                Log.e(TAG, "Exception on closing MD5 input stream", e)
            }
        }
    }

    fun calculateMD5(`is`: InputStream): String? {
        val digest: MessageDigest?
        try {
            digest = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Exception while getting digest", e)
            return null
        }

        val buffer = ByteArray(8192)
        var read: Int
        try {
            while ((`is`.read(buffer).also { read = it }) > 0) {
                digest.update(buffer, 0, read)
            }
            val md5sum = digest.digest()
            val bigInt = BigInteger(1, md5sum)
            var output = bigInt.toString(16)
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0')
            return output
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for MD5", e)
        } finally {
            try {
                `is`.reset()
            } catch (e: IOException) {
                Log.e(TAG, "Exception on closing MD5 input stream", e)
            }
        }
    }
}

