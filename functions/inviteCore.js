"use strict";

const crypto = require("node:crypto");

function createToken() {
  return crypto.randomBytes(32).toString("base64url");
}
function tokenHash(token) {
  if (typeof token !== "string" || !/^[A-Za-z0-9_-]{43}$/.test(token)) {
    throw new Error("invalid token");
  }
  return crypto.createHash("sha256").update(token, "utf8").digest("hex");
}
function requiredName(value, min = 2, max = 60) {
  if (typeof value !== "string" || value.trim().length < min || value.trim().length > max) {
    throw new Error("invalid name");
  }
  return value.trim();
}
function validColor(value) {
  if (typeof value !== "string" || !/^#[0-9a-fA-F]{6}$/.test(value)) {
    throw new Error("invalid color");
  }
  return value.toUpperCase();
}
function normalizeEmail(value) {
  if (!value) return null;
  if (typeof value !== "string") throw new Error("invalid email");
  const email = value.trim().toLowerCase();
  if (email.length > 254 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    throw new Error("invalid email");
  }
  return email;
}
function validBirthDate(value) {
  if (!value) return null;
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    throw new Error("invalid birth date");
  }
  const date = new Date(value + "T00:00:00.000Z");
  if (!Number.isFinite(date.getTime()) || date.toISOString().slice(0,10) !== value
      || date.getTime() > Date.now() || date.getUTCFullYear() < 1900) {
    throw new Error("invalid birth date");
  }
  return value;
}
/**
 * Les participants avec compte n'ont pas besoin d'être des adultes ni des parents.
 * "adult" est uniquement un alias conservé pour les anciens clients et invitations.
 * Le niveau d'accès est distinct du profil affiché dans le calendrier.
 */
function normalizeInviteRole(role) {
  if (role === "participant" || role === "adult") return "participant";
  if (role === "parent") return "parent"; // compatibilité des liens existants
  throw new Error("invalid invite role");
}
const isHolder = (role) => role === "owner" || role === "admin" || role === "parent";
const isFullHolderGroup = (roles) => roles.filter(isHolder).length >= 2;

module.exports = {
  createToken, tokenHash, requiredName, validColor, normalizeEmail,
  validBirthDate, isHolder, isFullHolderGroup, normalizeInviteRole
};
