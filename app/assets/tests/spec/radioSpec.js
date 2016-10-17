//specify custom path

var path = '';
if (typeof window.__karma__ !== 'undefined') {
  path += 'base/'
}

jasmine.getFixtures().fixturesPath = path + 'tests/fixtures';

var allTargetGroups = '#target0, #target1, #target2, #target3, #target4, #target5';

describe("Radio show hide test suite", function() {

	//startup
	beforeEach(function(){
		loadFixtures('radioFragment.html');
		//trigger functions
		VOA.VoaModules();
	});

	//teardown
	afterEach(function() {
	  	 $(document).off();
	});

	it('Namespaces should be available on the jQuery object', function() {
		expect(GOVUK, VOA).toBeDefined();
	});

	it("Should be able to set fixtures", function() {
        expect(setFixtures).toBeDefined();
    });

	describe("Radio show/hide fields", function() {

		it("All target groups should not be visible", function() {
			expect($(allTargetGroups)).not.toBeVisible();
	    });

		it("Given \"#radio_false\" is checked target group should visible", function() {
			$('#radio_false').prop('checked', true).change();
			expect($('#target0')).toBeVisible();
	    });

		it("Given \"#radio_true\" is checked target group should not be visible", function() {
			$('#radio_true').prop('checked', true).change();
			expect($('#target0, #target1, #target2, #target3')).not.toBeVisible();
	    });
    });

	describe("Radio show/hide multiple fields", function() {

		it("All target groups should not be visible", function() {
			expect($(allTargetGroups)).not.toBeVisible();
	    });

		it("Given \"#radio_maybe\" is checked target groups (#target1, #target2) should visible", function() {
			$('#radio_maybe').prop('checked', true).change();
			expect($('#target1, #target2')).toBeVisible();
	    });

		it("Given \"#radio_true\" is checked all target groups should not be visible", function() {
			$('#radio_true').prop('checked', true).change();
			expect($(allTargetGroups)).not.toBeVisible();
		});

    });

	describe("Radio show/hide fields level 2", function() {

		it("Target groups (#target3, #target4) should not be visible", function() {
			expect($('#target3, #target4')).not.toBeVisible();
	    });

		it("Given \"Show target 3\" is checked > \"level2_radio_false\" is checked #target3 and #target4 group should visible", function() {
			$('#radio_level2').prop('checked', true).change();
			expect($('#target3')).toBeVisible();
			$('#level2_radio_false').prop('checked', true).change();
			expect($('#target4')).toBeVisible();
	    });

    });

    describe("Radio show/hide fields level 3", function() {

		it("Target groups (#target3, #target4, #target4) should not be visible", function() {
			expect($('#target3, #target4, #target4')).not.toBeVisible();
	    });

		it("Given \"Show target 3\" is checked > \"level2_radio_false\" is checked > \"level3_radio_false\" is checked #target3, #target4, #target5 groups should be visible", function() {
			$('#radio_level2').prop('checked', true).change();
			expect($('#target3')).toBeVisible();
			$('#level2_radio_false').prop('checked', true).change();
			expect($('#target4')).toBeVisible();
			$('#level3_radio_false').prop('checked', true).change();
			expect($('#target5')).toBeVisible();
	    });

    });

});
