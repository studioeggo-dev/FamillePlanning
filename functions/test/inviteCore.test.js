"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const c = require("../inviteCore");
test("invitation token is strong and cannot be forged by weak strings",()=>{
  const token=c.createToken();
  assert.equal(token.length,43);
  assert.equal(c.tokenHash(token).length,64);
  assert.notEqual(c.tokenHash(token),token);
  assert.throws(()=>c.tokenHash("123"));
});
test("only two parent holders (owner + parent)",()=>{
  assert.equal(c.isFullHolderGroup(["owner"]),false);
  assert.equal(c.isFullHolderGroup(["owner","parent"]),true);
  assert.equal(c.isFullHolderGroup(["owner","adult"]),false);
});
test("a dependent has no requirement for an email",()=>{
  assert.equal(c.validBirthDate("2018-04-12"),"2018-04-12");
  assert.equal(c.validBirthDate(""),null);
  assert.throws(()=>c.validBirthDate("2018-02-31"));
  assert.throws(()=>c.validBirthDate("2099-01-01"));
});
test("name, color and optional email are validated",()=>{
  assert.equal(c.requiredName("  Emma  "),"Emma");
  assert.equal(c.validColor("#ff6b6b"),"#FF6B6B");
  assert.equal(c.normalizeEmail(" PERSON@EXAMPLE.ORG "),"person@example.org");
  assert.throws(()=>c.validColor("red"));
});

test("any invited participant can join without being a parent or an adult-only role", () => {
  assert.equal(c.normalizeInviteRole("participant"), "participant");
  assert.equal(c.normalizeInviteRole("adult"), "participant");
  assert.equal(c.normalizeInviteRole("parent"), "parent");
  assert.throws(() => c.normalizeInviteRole("owner"));
  assert.throws(() => c.normalizeInviteRole("admin"));
  assert.throws(() => c.normalizeInviteRole("child"));
  assert.equal(c.isFullHolderGroup(["owner", "participant", "participant"]), false);
});
